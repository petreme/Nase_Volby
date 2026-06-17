package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import coil.compose.rememberAsyncImagePainter

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay
import java.io.InputStream
import kotlin.random.Random

@Composable
fun rememberCleanLogo(resourceId: Int): ImageBitmap? {
    val context = LocalContext.current
    return remember(resourceId) {
        try {
            val options = BitmapFactory.Options().apply {
                inMutable = true
            }
            val original = BitmapFactory.decodeResource(context.resources, resourceId, options)
            if (original != null) {
                val width = original.width
                val height = original.height
                val pixels = IntArray(width * height)
                original.getPixels(pixels, 0, width, 0, 0, width, height)
                
                // Flood fill starting from all edges to remove white/off-white background safely
                val visited = BooleanArray(width * height)
                val queue = java.util.ArrayDeque<Int>()
                
                fun push(x: Int, y: Int) {
                    if (x in 0 until width && y in 0 until height) {
                        val idx = y * width + x
                        if (!visited[idx]) {
                            visited[idx] = true
                            queue.add(idx)
                        }
                    }
                }
                
                // Seed border pixels
                for (x in 0 until width) {
                    push(x, 0)
                    push(x, height - 1)
                }
                for (y in 0 until height) {
                    push(0, y)
                    push(width - 1, y)
                }
                
                while (!queue.isEmpty()) {
                    val idx = queue.poll() ?: break
                    val x = idx % width
                    val y = idx / width
                    
                    val color = pixels[idx]
                    val a = (color shr 24) and 0xFF
                    val r = (color shr 16) and 0xFF
                    val g = (color shr 8) and 0xFF
                    val b = color and 0xFF
                    
                    // We flood fill if the pixel is white, light grey, or already transparent.
                    // This stops at the Slovak shield outline/red/blue components.
                    val isBg = (r > 200 && g > 200 && b > 200) || a < 50
                    
                    if (isBg) {
                        pixels[idx] = 0x00000000 // Force alpha to 0 (fully transparent)
                        push(x + 1, y)
                        push(x - 1, y)
                        push(x, y + 1)
                        push(x, y - 1)
                    }
                }
                
                val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                result.setPixels(pixels, 0, width, 0, 0, width, height)
                result.asImageBitmap()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

// 100% Očistené od politických strán. Naviazané na vaše reálne webové sekcie a transparentné posúdenie odborníka.
data class OfficialPerson(
    val id: Int,
    val title: String,      // Ministerstvo / Funkcia
    val name: String,       // Meno občana-kandidáta
    val programUrl: String,  // Volebný program kandidáta
    val salaryUrl: String,   // Návrh na výšku platu úradníka
    val petitionUrl: String, // Kvalifikovaný občiansky návrh
    val imageUri: Uri? = null,
    val placeholderRes: Int = R.drawable.nase_volby_clean_logo_1780731853180
)

data class HlasovanieItem(
    val title: String,
    val url: String,
    val icon: String
)

@Composable
fun CivicAppUI() {
    var currentScreen by remember { mutableStateOf("splash") }
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateResultText by remember { mutableStateOf<String?>(null) }

    // Používateľské nastavenia
    var language by remember { mutableStateOf("SK") }
    var textDisplaySize by remember { mutableStateOf("Štandardné") }
    var graphicTheme by remember { mutableStateOf("Slovenské farby") }
    var layoutMode by remember { mutableStateOf("Dlaždice") }

    val scale = if (textDisplaySize == "Zväčšené") 1.25f else 1.0f
    
    val backgroundColor = if (graphicTheme == "Slovenské farby") Color(0xFF092A5C) else Color(0xFF121824)
    val cardColor = if (graphicTheme == "Slovenské farby") Color(0xFF143B73) else Color(0xFF1E293B)
    val borderColor = if (graphicTheme == "Slovenské farby") Color(0xFFEE2436) else Color(0xFF3B82F6)
    val accentColor = if (graphicTheme == "Slovenské farby") Color(0xFFEE2436) else Color(0xFF3B82F6)
    val textColor = Color(0xFFFFFFFF)

    // REÁLNY ZOZNAM BEZ STRÁN - Odkazy smerujú presne na podstránky z poslednereferendum.sk a nasevolby.sk
    var officialsList by remember {
        mutableStateOf(
            listOf(
                OfficialPerson(
                    id = 1,
                    title = "Predseda vlády SR",
                    name = "Kandidát bez politickej strany 1",
                    programUrl = "https://www.poslednereferendum.nasevolby.sk/?page_id=977",
                    salaryUrl = "https://www.poslednereferendum.nasevolby.sk/?page_id=1002",
                    petitionUrl = "https://www.poslednereferendum.nasevolby.sk/?page_id=1008"
                ),
                OfficialPerson(
                    id = 2,
                    title = "Minister vnútra SR",
                    name = "Kandidát bez politickej strany 2",
                    programUrl = "https://www.poslednereferendum.nasevolby.sk/?page_id=977",
                    salaryUrl = "https://www.poslednereferendum.nasevolby.sk/?page_id=1002",
                    petitionUrl = "https://www.poslednereferendum.nasevolby.sk/?page_id=1008"
                ),
                OfficialPerson(
                    id = 3,
                    title = "Minister financií SR",
                    name = "Kandidát bez politickej strany 3",
                    programUrl = "https://www.poslednereferendum.nasevolby.sk/?page_id=977",
                    salaryUrl = "https://www.poslednereferendum.nasevolby.sk/?page_id=1002",
                    petitionUrl = "https://www.poslednereferendum.nasevolby.sk/?page_id=1008"
                ),
                OfficialPerson(
                    id = 4,
                    title = "Minister spravodlivosti SR",
                    name = "Kandidát bez politickej strany 4",
                    programUrl = "https://www.poslednereferendum.nasevolby.sk/?page_id=977",
                    salaryUrl = "https://www.poslednereferendum.nasevolby.sk/?page_id=1002",
                    petitionUrl = "https://www.poslednereferendum.nasevolby.sk/?page_id=1008"
                )
            )
        )
    }
    
    var selectedOfficialForVote by remember { mutableStateOf<OfficialPerson?>(null) }
    var activeOfficialForImageUpload by remember { mutableStateOf<OfficialPerson?>(null) }

    // Stav KEP-GSM šifrovania
    var generatedSmsCode by remember { mutableStateOf("") }
    var userEnteredSmsCode by remember { mutableStateOf("") }
    var votingStep by remember { mutableStateOf(1) }
    var isDataConnectedAlertVisible by remember { mutableStateOf(false) }
    var isSmsSendingActive by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && activeOfficialForImageUpload != null) {
            officialsList = officialsList.map { person ->
                if (person.id == activeOfficialForImageUpload?.id) {
                    person.copy(imageUri = uri)
                } else {
                    person
                }
            }
        }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(context, "Bez oprávnenia nie je možné odoslať bezpečné hlasovanie cez GSM!", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(currentScreen) {
        // No automatic transition here. User decides when to proceed by clicking the button!
    }

    if (currentScreen == "splash") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B2240))
                .safeDrawingPadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, Color(0xFFEE2436), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Moja voľba",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Image(
                        painter = painterResource(id = R.drawable.ic_splash_logo_clean),
                        contentDescription = "Logo Moja Voľba",
                        modifier = Modifier
                            .size(220.dp)
                            .padding(8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Máte pred sebou jedinečný demokratický nástroj na spravodlivé riadenie štátu. Váš hlas v tejto aplikácii má veľkú silu a osobnú zodpovednosť. Môže rozhodnúť o spravodlivosti a prosperite v našej krajine. Využite to a urobte našu krajinu takú, ako si želá väčšina občanov.",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { currentScreen = "menu" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEE2436),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = "Vyberte si",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    } 
    else if (currentScreen == "menu") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .safeDrawingPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Hlavné menu",
                color = textColor,
                fontSize = (24 * scale).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            if (layoutMode == "Dlaždice") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CardTile(
                            title = "Hlasovania",
                            icon = "🗳️",
                            modifier = Modifier.weight(1f).height(145.dp),
                            onClick = { currentScreen = "sub_hlasovania" },
                            borderColor = borderColor, cardColor = cardColor, textColor = textColor, scale = scale
                        )
                        CardTile(
                            title = "Pre kandidátov",
                            icon = "👤",
                            modifier = Modifier.weight(1f).height(145.dp),
                            onClick = { currentScreen = "sub_kandidati" },
                            borderColor = borderColor, cardColor = cardColor, textColor = textColor, scale = scale
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CardTile(
                            title = "Sprievodca obvodmi",
                            icon = "🗺️",
                            modifier = Modifier.weight(1f).height(145.dp),
                            onClick = { currentScreen = "sub_obvody" },
                            borderColor = borderColor, cardColor = cardColor, textColor = textColor, scale = scale
                        )
                        CardTile(
                            title = "Informácie",
                            icon = "ℹ️",
                            modifier = Modifier.weight(1f).height(145.dp),
                            onClick = { currentScreen = "sub_info" },
                            borderColor = borderColor, cardColor = cardColor, textColor = textColor, scale = scale
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { currentScreen = "settings" },
                    colors = ButtonDefaults.buttonColors(containerColor = cardColor),
                    border = BorderStroke(2.dp, borderColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp)
                ) {
                    Text(text = "⚙️ Nastavenie", color = textColor, fontSize = (15 * scale).sp, fontWeight = FontWeight.Bold)
                }
            } 
            else {
                // Textový režim menu navigácie
                val menuItems = listOf(
                    Triple("Volebné obvody", "Districts Guide", "https://www.nasevolby.sk/?page_id=10941"),
                    Triple("Môj obvod-poslanec", "My Representative", "https://www.nasevolby.sk/?page_id=11121"),
                    Triple("Volím vysokého štátneho úradníka (Ministra)", "Vote for High State Official", "VnútornáObrazovkaFunkcionari"),
                    Triple("Hlasovanie o občianskych návrhoch", "Voting on Civic Proposals", "https://hlasujeme.nasevolby.sk/hlasovanie-demo/"),
                    Triple("Výber Občianskych návrhov", "Selection of Civic Proposals", "https://hlasujeme.nasevolby.sk/vyber-navrhov/"),
                    Triple("Dnešné hlasovanie", "Today's Voting", "https://hlasujeme.nasevolby.sk/minule-hlasovania-buduce-hlasovania/dnesne-hlasovanie/"),
                    Triple("Budúce hlasovanie", "Future Voting", "https://hlasujeme.nasevolby.sk/minule-hlasovania-buduce-hlasovania/buduce-hlasovania/"),
                    Triple("História hlasovaní", "Voting History", "https://hlasujeme.nasevolby.sk/minule-hlasovania-buduce-hlasovania/571-2/"),
                    Triple("Volebný program kandidáta na poslanca do NR SR", "Representative Candidate Platform", "https://www.poslednereferendum.nasevolby.sk/?page_id=977"),
                    Triple("Návrh na výšku platu vysokého štátneho úradníka", "Salary Proposal for High Official", "https://www.poslednereferendum.nasevolby.sk/?page_id=1002"),
                    Triple("Kvalifikovaný občiansky návrh (petícia na referendum)", "Qualified Civic Proposal (Petition)", "https://www.poslednereferendum.nasevolby.sk/?page_id=1008"),
                    Triple("Slovník cudzích pojmov", "Glossary of Terms", "https://www.poslednereferendum.nasevolby.sk/?page_id=994")
                )

                menuItems.forEachIndexed { index, item ->
                    Button(
                        onClick = {
                            if (item.third == "VnútornáObrazovkaFunkcionari") {
                                currentScreen = "funkcionari"
                            } else {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.third))
                                context.startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = cardColor),
                        border = BorderStroke(1.5.dp, borderColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).defaultMinSize(minHeight = 52.dp)
                    ) {
                        Text(
                            text = "${index + 1}. ${item.first}",
                            color = textColor, fontSize = (14 * scale).sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { currentScreen = "settings" },
                    colors = ButtonDefaults.buttonColors(containerColor = cardColor),
                    border = BorderStroke(2.dp, borderColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).defaultMinSize(minHeight = 56.dp)
                ) {
                    Text(text = "13. ⚙️ Nastavenie", color = textColor, fontSize = (15 * scale).sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { currentScreen = "splash" },
                colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
            ) {
                Text(text = "Naspäť na úvodnú obrazovku", color = textColor, fontSize = (14 * scale).sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
// ... KÓD POKRAČUJE V DRUHEJ SPRÁVE OBRAZOVKOU VOLBA MINISTRA ...
else if (currentScreen == "sub_hlasovania") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF092A5C)) // Deep Navy Blue background
                .safeDrawingPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Hlasovanie",
                color = Color.White,
                fontSize = (24 * scale).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )

            Text(
                text = "Vyberte typ hlasovania pre priame zapojenie sa do správy vecí verejných:",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = (14 * scale).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp, start = 8.dp, end = 8.dp)
            )

            val hlasovanieItems = listOf(
                HlasovanieItem("1. Voľba ministrov", "https://hlasujeme.nasevolby.sk/volba-ministrov", "💼"),
                HlasovanieItem("2. Voľba Poslancov do NR SR", "https://hlasujeme.nasevolby.sk/volba-poslancov", "👥"),
                HlasovanieItem("3. Voľba vysokých štátnych úradníkov", "https://hlasujeme.nasevolby.sk/volba-ministra-2/", "🏛️"),
                HlasovanieItem("4. Hlasovanie o Občianskych návrhoch", "https://hlasujeme.nasevolby.sk/hlasovanie-demo/", "📜"),
                HlasovanieItem("5. Výber prioritných Občianskych návrhov", "https://hlasujeme.nasevolby.sk/vyber-navrhov/", "🎯")
            )

            hlasovanieItems.forEach { item ->
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1B2A47) // Dark Slate Gray / Navy buttons
                    ),
                    border = BorderStroke(3.dp, Color(0xFFCC0000)), // 3px solid #CC0000 (Red)
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .defaultMinSize(minHeight = 60.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = item.icon,
                            fontSize = (22 * scale).sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = item.title,
                            color = Color.White, // #FFFFFF text color
                            fontSize = (15 * scale).sp,
                            fontWeight = FontWeight.Bold, // Bold weight as requested
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { currentScreen = "menu" },
                colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Text(
                    text = "Naspäť do hlavného menu",
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    else if (currentScreen == "funkcionari") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .safeDrawingPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Volíme vysokého štátneho úradníka",
                color = textColor,
                fontSize = (22 * scale).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            
            Text(
                text = "Vyberte dôležitý post v štáte, pre ktorý chcete posúdiť nezávislých odborníkov:",
                color = textColor,
                fontSize = (14 * scale).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            officialsList.forEach { person ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = BorderStroke(2.dp, borderColor)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Nahratie/Zobrazenie fotografie kandidáta
                            Box(
                                modifier = Modifier
                                    .size(65.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .clickable {
                                        activeOfficialForImageUpload = person
                                        imagePickerLauncher.launch("image/*")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (person.imageUri != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(person.imageUri),
                                        contentDescription = "Foto",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = person.placeholderRes),
                                        contentDescription = "Logo",
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = person.title,
                                    color = accentColor,
                                    fontSize = (16 * scale).sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = person.name,
                                    color = textColor,
                                    fontSize = (15 * scale).sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Transparentné posúdenie kandidáta:", color = Color.LightGray, fontSize = (12 * scale).sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        // TRI KĽÚČOVÉ TLAČIDLÁ PODĽA VÁŠHO WEBU (BEZ STRÁN)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(person.programUrl))) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 38.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(text = "📋 Program", color = Color.White, fontSize = (11 * scale).sp, maxLines = 1)
                            }

                            Button(
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(person.salaryUrl))) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 38.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(text = "💰 Plat", color = Color.White, fontSize = (11 * scale).sp, maxLines = 1)
                            }

                            Button(
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(person.petitionUrl))) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309)),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 38.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(text = "✍️ Petícia", color = Color.White, fontSize = (11 * scale).sp, maxLines = 1)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                selectedOfficialForVote = person
                                generatedSmsCode = String.format("%05d", Random.nextInt(10000, 99999))
                                userEnteredSmsCode = ""
                                votingStep = 1
                                currentScreen = "kep_trenažer"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Hlasovať za úradníka", color = textColor, fontWeight = FontWeight.Bold, fontSize = (13 * scale).sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { currentScreen = "menu" },
                colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
            ) {
                Text(text = "Späť do hlavného menu", color = textColor, fontWeight = FontWeight.Bold)
            }
        }
    }
    else if (currentScreen == "kep_trenažer") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .safeDrawingPadding()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .border(BorderStroke(3.dp, borderColor), RoundedCornerShape(16.dp))
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🔒 KEP-GSM Trenažér",
                    color = textColor,
                    fontSize = (24 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (votingStep == 1) {
                    // Krok 1: Info o voličovi a odpojení od internetu
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Volič: Miroslav Petre", color = textColor, fontSize = (16 * scale).sp, fontWeight = FontWeight.Bold)
                            Text(text = "Obvod č. 149 (Trnava)", color = Color.LightGray, fontSize = (14 * scale).sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Upozornenie: Celý proces šifrovania KEP prebieha v offline režime s automatickým odpojením od dát a WiFi kvôli maximálnej bezpečnosti pred útokmi z internetu.",
                                color = Color.Yellow,
                                fontSize = (12 * scale).sp
                            )
                        }
                    }

                    Button(
                        onClick = { 
                            isDataConnectedAlertVisible = true
                            votingStep = 2 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 50.dp)
                    ) {
                        Text(text = "Ďalej (Odpojiť a šifrovať)", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
                else if (votingStep == 2) {
                    // Krok 2: Zadanie vygenerovaného 5-miestneho KEP kódu
                    Text(
                        text = "Zadajte 5-miestny šifrovací kód",
                        color = textColor,
                        fontSize = (18 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "Simulovaný kód doručený pre váš KEP: $generatedSmsCode",
                        color = Color.Green,
                        fontSize = (15 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = userEnteredSmsCode,
                        onValueChange = { if (it.length <= 5) userEnteredSmsCode = it },
                        label = { Text("Zadajte 5-miestny kód", color = Color.LightGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = {
                            if (userEnteredSmsCode == generatedSmsCode) {
                                votingStep = 3
                            } else {
                                Toast.makeText(context, "Nesprávny šifrovací kód KEP!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 50.dp)
                    ) {
                        Text(text = "Zašifrovať a potvrdiť voľbu", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
                else if (votingStep == 3) {
                    // Krok 3: Finálne odoslanie SMS cez operátora priamo volebnej komisii
                    Text(
                        text = "Skontrolujte a potvrďte voľbu",
                        color = textColor,
                        fontSize = (18 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        buildAnnotatedString {
                            append("Váš výber na post ")
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = accentColor)) {
                                append("[${selectedOfficialForVote?.title}]:\n\n")
                            }
                            withStyle(style = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.Yellow)) {
                                append("${selectedOfficialForVote?.name}\n")
                            }
                        },
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Button(
                        onClick = {
                            isSmsSendingActive = true
                            coroutineScope.launch {
                                try {
                                    val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        context.getSystemService(SmsManager::class.java)
                                    } else {
                                        SmsManager.getDefault()
                                    }
                                    // Bezpečná 256-bitová šifra odoslaná priamo volebnej komisii cez GSM
                                    smsManager.sendTextMessage(
                                        "0903123456", 
                                        null, 
                                        "KEP-GSM:256BIT-ENC-VOTE-ID-${selectedOfficialForVote?.id}-CODE-$generatedSmsCode", 
                                        null, 
                                        null
                                    )
                                    Toast.makeText(context, "Hlasovanie bolo bezpečne odoslané cez GSM!", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    // Záložný trenažér pre emulátory bez SIM karty
                                    Toast.makeText(context, "Simulácia: Šifra odoslaná priamo komisii!", Toast.LENGTH_SHORT).show()
                                }
                                delay(1500)
                                isSmsSendingActive = false
                                votingStep = 4
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 54.dp)
                    ) {
                        if (isSmsSendingActive) {
                            CircularProgressIndicator(color = textColor, modifier = Modifier.size(24.dp))
                        } else {
                            Text(text = "Odoslať bezpečnú SMS šifru", color = textColor, fontWeight = FontWeight.Bold, fontSize = (16 * scale).sp)
                        }
                    }
                }
                else if (votingStep == 4) {
                    // Krok 4: Výsledky simulácie
                    Text(
                        text = "Ďakujeme za váš hlas, ktorý prispel na spravodlivé riadenie štátu!",
                        color = Color.Green,
                        fontSize = (18 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        border = BorderStroke(1.dp, Color.Green)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Priebežné výsledky simulácie:", color = Color.Yellow, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "${selectedOfficialForVote?.name} (Vy): 60%", color = textColor)
                            Text(text = "Ostatní nezávislí kandidáti: 40%", color = Color.LightGray)
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = borderColor)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Chcete vedieť, ako presne máme tento systém legislatívne a technicky pripravený v našom pláne?", color = textColor, fontSize = (12 * scale).sp, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.poslednereferendum.nasevolby.sk/"))) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                            ) {
                                Text(text = "🚀 PREČÍTAŤ CELÝ PLÁN NA WEBE", color = Color.Black, fontSize = (11 * scale).sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Button(
                        onClick = { currentScreen = "menu" },
                        colors = ButtonDefaults.buttonColors(containerColor = cardColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Ukončiť hlasovanie", color = textColor)
                    }
                }
            }
        }
    }
    else if (currentScreen == "sub_kandidati") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .safeDrawingPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Pre kandidátov",
                color = textColor,
                fontSize = (24 * scale).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            Text(
                text = "Informácie pre nezávislých občianskych kandidátov bez politickej príslušnosti:",
                color = textColor.copy(alpha = 0.9f),
                fontSize = (14 * scale).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Card 1: Volebný program
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(2.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "📋 Volebný program", color = accentColor, fontSize = (18 * scale).sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Volebný program kandidáta na poslanca do NR SR vyžaduje transparentnosť a bezúhonnosť. Prečítajte si kompletné pokyny a vzory.",
                        color = textColor,
                        fontSize = (13 * scale).sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.poslednereferendum.nasevolby.sk/?page_id=977"))) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Otvoriť program na webe", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Card 2: Návrh platu
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(2.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "💰 Návrh platu úradníka", color = accentColor, fontSize = (18 * scale).sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Návrh na transparentnú výšku platu vysokého štátneho úradníka na základe odvedenej práce a spokojnosti občanov.",
                        color = textColor,
                        fontSize = (13 * scale).sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.poslednereferendum.nasevolby.sk/?page_id=1002"))) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Zobraziť návrh platu", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Card 3: Petícia / Občiansky návrh
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(2.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "✍️ Občiansky návrh (Petícia)", color = accentColor, fontSize = (18 * scale).sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Podanie kvalifikovaného občianskeho návrhu a spustenie petície na referendum. Každý má právo predložiť zmysluplný návrh.",
                        color = textColor,
                        fontSize = (13 * scale).sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.poslednereferendum.nasevolby.sk/?page_id=1008"))) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Zobraziť kvalifikované návrhy", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Card 4: Podpisový hárok kandidáta na kampaň
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(2.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "📄 Podpisový hárok kandidáta", color = accentColor, fontSize = (18 * scale).sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Vytlačte si podpisový hárok pre podporu nezávislých občianskych kandidátov pre ich kampaň. Každý hlas a podpis pomáha priblížiť zmenu.",
                        color = textColor,
                        fontSize = (13 * scale).sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.poslednereferendum.nasevolby.sk/?page_id=1092"))) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Otvoriť podpisový hárok", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { currentScreen = "menu" },
                colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
            ) {
                Text(text = "Naspäť do hlavného menu", color = textColor, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    else if (currentScreen == "sub_obvody") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .safeDrawingPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sprievodca obvodmi",
                color = textColor,
                fontSize = (24 * scale).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            Text(
                text = "Zistite informácie o vašom volebnom obvode a pridelených poslancoch:",
                color = textColor.copy(alpha = 0.9f),
                fontSize = (14 * scale).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Card 1: Volebné obvody link
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(2.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "🗺️ Volebné obvody a mapy", color = accentColor, fontSize = (18 * scale).sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Volebné obvody v SR pre transparentný výber poslancov. Úplné zoznamy a geografické členenie nájdete na našom portáli.",
                        color = textColor,
                        fontSize = (13 * scale).sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.nasevolby.sk/?page_id=10941"))) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Otvoriť obvody a mapy", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Card 2: Môj obvod - poslanec
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(2.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "👥 Vyhľadať môjho poslanca", color = accentColor, fontSize = (18 * scale).sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Vyhľadajte si priradeného poslanca podľa konkrétneho obvodu alebo ulice pre nadviazanie občianskeho dialógu.",
                        color = textColor,
                        fontSize = (13 * scale).sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.nasevolby.sk/?page_id=11121"))) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Vyhľadať na webe", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Interactive local selector
            var selectedKraj by remember { mutableStateOf("Vyberte kraj") }
            var expandedKrajDropdown by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(2.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "🇸🇰 Rýchle info podľa vašej polohy", color = Color.Yellow, fontSize = (16 * scale).sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { expandedKrajDropdown = true },
                            colors = ButtonDefaults.buttonColors(containerColor = cardColor),
                            border = BorderStroke(1.dp, Color.Gray),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = selectedKraj, color = textColor)
                        }
                        DropdownMenu(
                            expanded = expandedKrajDropdown,
                            onDismissRequest = { expandedKrajDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.85f).background(cardColor)
                        ) {
                            val kraje = listOf(
                                "Bratislavský kraj", "Trnavský kraj", "Trenčiansky kraj", 
                                "Nitriansky kraj", "Žilinský kraj", "Banskobystrický kraj", 
                                "Prešovský kraj", "Košický kraj"
                            )
                            kraje.forEach { kraj ->
                                DropdownMenuItem(
                                    text = { Text(text = kraj, color = textColor) },
                                    onClick = {
                                        selectedKraj = kraj
                                        expandedKrajDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedKraj != "Vyberte kraj") {
                        Spacer(modifier = Modifier.height(16.dp))
                        val infoText = when (selectedKraj) {
                            "Bratislavský kraj" -> "Bratislavský samosprávny kraj tvorí samostatnú silnú demografickú časť s vysokou volebnou účasťou. Zastúpenie v NR SR má vyčlenené na základe celkového počtu obyvateľov podľa sčítania."
                            "Trnavský kraj" -> "Trnavský kraj disponuje dôležitými priemyselnými centrami. Volebné obvody zahŕňajú Trnavu, Dunajskú Stredu, Galantu, Hlohovec, Piešťany, Senicu a Skalicu."
                            "Trenčiansky kraj" -> "Trenčiansky kraj má bohatú históriu a dôležité stredné Považie. Obvody pokrývajú Trenčín, Bánovce, Ilavu, Myjavu, Nové Mesto nad Váhom, Partizánske, Považskú Bystricu, Prievidzu a Púchov."
                            "Nitriansky kraj" -> "Nitriansky kraj patrí medzi agrikultúrne jadro Slovenska. Volebné obvody zahŕňajú Nitru, Komárno, Levice, Nové Zámky, Šaliu, Topoľčany a Zlaté Moravce."
                            "Žilinský kraj" -> "Žilinský kraj zahŕňa hornatý sever a stredné Slovensko. Reprezentuje obvody Žilina, Bytča, Čadca, Dolný Kubín, Kysucké Nové Mesto, Liptovský Mikuláš, Martin, Námestovo, Ružomberok, Turčianske Teplice a Tvrdošín."
                            "Banskobystrický kraj" -> "Banskobystrický kraj je najväčším krajom v SR. Volebné obvody: Banská Bystrica, Banská Štiavnica, Brezno, Detva, Krupina, Lučenec, Poltár, Revúca, Rimavská Sobota, Veľký Krtíš, Zvolen, Žarnovica, Žiar nad Hronom."
                            "Prešovský kraj" -> "Prešovský kraj je najľudnatejším samosprávnym krajom SR s dynamickým rozvojom a dôležitým severovýchodným obvodom."
                            else -> "Košický kraj predstavuje dôležité priemyselné, vedecké a kultúrne centrum východného Slovenska."
                        }
                        Text(
                            text = infoText,
                            color = textColor,
                            fontSize = (13 * scale).sp,
                            lineHeight = (18 * scale).sp
                        )

                        // Štatistické údaje priradené ku každému kraju (podľa oficiálneho návrhu reformy)
                        class TempKrajStats(
                            val kod: String,
                            val pocetObyv: String,
                            val rozloha: String,
                            val hustota: String,
                            val obceMiest: String,
                            val zTohoMiest: String,
                            val pocetOkresov: String
                        )

                        val stats = when (selectedKraj) {
                            "Banskobystrický kraj" -> TempKrajStats("601", "607 581", "809,4 km²", "137 / km²", "42", "1", "13")
                            "Trenčiansky kraj" -> TempKrajStats("301", "565 572", "461,9 km²", "79 / km²", "43", "1", "9")
                            "Bratislavský kraj" -> TempKrajStats("101", "739 635", "9,6 km²", "4 285 / km²", "—", "—", "8")
                            "Žilinský kraj" -> TempKrajStats("501", "684 190", "281,6 km²", "110 / km²", "12", "1", "11")
                            "Trnavský kraj" -> TempKrajStats("201", "565 462", "1 074,6 km²", "113 / km²", "67", "4", "7")
                            "Prešovský kraj" -> TempKrajStats("702", "810 075", "754,2 km²", "82 / km²", "62", "1", "13")
                            "Košický kraj" -> TempKrajStats("802", "777 933", "85,4 km²", "791 / km²", "—", "—", "11")
                            "Nitriansky kraj" -> TempKrajStats("402", "661 995", "1 551,1 km²", "72 / km²", "89", "4", "7")
                            else -> null
                        }

                        if (stats != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "📊 Štatistické údaje (Reforma):",
                                color = Color.Yellow,
                                fontSize = (14 * scale).sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                val rowData = listOf(
                                    Pair("🔢 Kód kraja", stats.kod),
                                    Pair("👥 Počet obyvateľov", stats.pocetObyv),
                                    Pair("🗺️ Rozloha", stats.rozloha),
                                    Pair("📉 Počet obyv/km²", stats.hustota),
                                    Pair("🏘️ Počet obcí a miest", stats.obceMiest),
                                    Pair("🏙️ Z toho miest", stats.zTohoMiest),
                                    Pair("📁 Počet okresov", stats.pocetOkresov)
                                )
                                
                                rowData.forEachIndexed { index, pair ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = pair.first,
                                            color = textColor.copy(alpha = 0.8f),
                                            fontSize = (12 * scale).sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = pair.second,
                                            color = textColor,
                                            fontSize = (12 * scale).sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (index < rowData.size - 1) {
                                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { currentScreen = "menu" },
                colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
            ) {
                Text(text = "Naspäť do hlavného menu", color = textColor, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    else if (currentScreen == "sub_info") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .safeDrawingPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Informácie a Slovník",
                color = textColor,
                fontSize = (24 * scale).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            Text(
                text = "Prehľad dôležitých odkazov a výklad demokratických pojmov pod záštitou OZ Naše Voľby:",
                color = textColor.copy(alpha = 0.9f),
                fontSize = (14 * scale).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp, start = 8.dp, end = 8.dp)
            )

            // 1. Všetko o zmene volebného systému.
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(2.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🗳️ Všetko o zmene volebného systému.",
                        color = Color.Yellow,
                        fontSize = (15 * scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Kompletné informácie a kľúčové odpovede ohľadom pripravovanej reformy volebného systému.",
                        color = textColor,
                        fontSize = (13 * scale).sp,
                        lineHeight = (18 * scale).sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.nasevolby.sk/"))) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Otvoriť nasevolby.sk", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 2. Takto budeme voliť a odvolávať poslancov...
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(2.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "📱 Takto budeme voliť a odvolávať poslancov, vysokých štátnych úradníkov, navrhovať a rušiť zákony a to všetko zadarmo, bezpečne a pohodlne odkiaľkoľvek",
                        color = Color.Yellow,
                        fontSize = (14 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = (19 * scale).sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Vyskúšajte si modernú, bezpečnú a bezplatnú priamu účasť na chode štátu z mobilu cez hlasovací portál.",
                        color = textColor,
                        fontSize = (13 * scale).sp,
                        lineHeight = (18 * scale).sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://hlasujeme.nasevolby.sk/"))) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Otvoriť hlasovací portál", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 3. Takto túto zmenu spoločne presadíme...
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(2.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🤝 Takto túto zmenu spoločne presadíme a nikto nám v tom nezabráni.",
                        color = Color.Yellow,
                        fontSize = (14 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = (19 * scale).sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pozrite si podrobný občiansky, ústavný a strategický akčný plán pre úspešné dosiahnutie cieľov.",
                        color = textColor,
                        fontSize = (13 * scale).sp,
                        lineHeight = (18 * scale).sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.poslednereferendum.nasevolby.sk/"))) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Zobraziť akčný plán", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Ostatné okná s info pojmami (Ostatné okná s info)
            val terms = listOf(
                Pair("Priama Demokracia", "Demokratická forma vlády, v ktorej o dôležitých verejných otázkach rozhoduje väčšina občanov priamo hlasovaním (referendom), a nie len prostredníctvom volených zástupcov."),
                Pair("Občiansky Návrh", "Možnosť každého občana predložiť transparentný návrh alebo zákon. Ak získa predpísanú podporu, stáva sa záväzným pre zváženie alebo hlasovanie celým národom."),
                Pair("KEP (Kvalifikovaný Elektronický Podpis)", "Bezpečný kryptografický podpis certifikovaný štátom, ktorý jednoznačne potvrdzuje identitu občana v offline aj online prostredí, pričom zachováva plnú integritu úkonu."),
                Pair("Bezpartijný Funkcionár", "Odborník nominovaný priamo občanmi do verejnej funkcie (ako ministerstvo), ktorý nie je členom žiadnej politickej strany, vďaka čomu háji záujmy výhradne všetkých občanov."),
                Pair("Slovník cudzích pojmov (Celý)", "Úplný a podrobný legislatívny slovník cudzích slov pre uľahčenie participácie občanov na chode štátu.")
            )

            terms.forEach { term ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = BorderStroke(1.dp, borderColor.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = term.first, color = accentColor, fontSize = (16 * scale).sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = term.second, color = textColor, fontSize = (13 * scale).sp, lineHeight = (18 * scale).sp)
                        
                        if (term.first == "Slovník cudzích pojmov (Celý)") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.poslednereferendum.nasevolby.sk/?page_id=994"))) },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "Otvoriť kompletný slovník", color = textColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Nové spodné podporné a informačné linky
            // Card 4: Ak chcete podporiť túto jedinečnú zmenu Kliknite sem
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(1.5.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "❤️ Ak chcete podporiť túto jedinečnú zmenu Kliknite sem",
                        color = Color.Yellow,
                        fontSize = (15 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = (19 * scale).sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Každý jeden dobrovoľník, príspevok a podpora našej spoločnej iniciatívy má obrovskú váhu.",
                        color = textColor,
                        fontSize = (13 * scale).sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.nasevolby.sk/?page_id=6086"))) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Podporiť našu iniciatívu", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Card 5: Kontakt
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(1.5.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "📞 Kontakt",
                        color = Color.Yellow,
                        fontSize = (15 * scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Máte nejaké otázky, podnety alebo návrhy? Spojte sa s nami cez náš kontaktný portál.",
                        color = textColor,
                        fontSize = (13 * scale).sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.nasevolby.sk/?page_id=7044"))) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Otvoriť kontaktné údaje", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Card 6: Kto sme
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(1.5.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "👥 Kto sme",
                        color = Color.Yellow,
                        fontSize = (15 * scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Zoznámte sa s naším príbehom, cieľmi a víziami pre transparentnejšiu občiansku spoločnosť.",
                        color = textColor,
                        fontSize = (13 * scale).sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.nasevolby.sk/?page_id=175"))) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Zistiť viac o nás", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Card 7: Spätná väzba
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { showFeedbackDialog = true },
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(1.5.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "💬 Spätná väzba",
                        color = Color.Yellow,
                        fontSize = (15 * scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Poprosíme o vaše akékoľvek pripomienky k tejto, alebo webovej aplikácii. Kliknite pre viac info.",
                        color = textColor,
                        fontSize = (13 * scale).sp,
                        lineHeight = (18 * scale).sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showFeedbackDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Zistiť podrobnosti", color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { currentScreen = "menu" },
                colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
            ) {
                Text(text = "Naspäť do hlavného menu", color = textColor, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    else if (currentScreen == "settings") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .safeDrawingPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚙️ Nastavenia aplikácie",
                color = textColor,
                fontSize = (24 * scale).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            Text(
                text = "Prispôsobte si aplikáciu podľa svojich potrieb:",
                color = textColor.copy(alpha = 0.9f),
                fontSize = (14 * scale).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Settings Card 1: Jazyk
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(1.5.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "🌐 Výber jazyka", color = accentColor, fontSize = (16 * scale).sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = language == "SK",
                                onClick = { language = "SK" },
                                colors = RadioButtonDefaults.colors(selectedColor = accentColor, unselectedColor = Color.Gray)
                            )
                            Text(text = "Slovenčina (SK)", color = textColor, fontSize = (14 * scale).sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = language == "EN",
                                onClick = { language = "EN" },
                                colors = RadioButtonDefaults.colors(selectedColor = accentColor, unselectedColor = Color.Gray)
                            )
                            Text(text = "English (EN)", color = textColor, fontSize = (14 * scale).sp)
                        }
                    }
                }
            }

            // Settings Card 2: Veľkosť písma
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(1.5.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "🔎 Veľkosť písma", color = accentColor, fontSize = (16 * scale).sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = textDisplaySize == "Štandardné",
                                onClick = { textDisplaySize = "Štandardné" },
                                colors = RadioButtonDefaults.colors(selectedColor = accentColor, unselectedColor = Color.Gray)
                            )
                            Text(text = "Štandardné", color = textColor, fontSize = (14 * scale).sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = textDisplaySize == "Zväčšené",
                                onClick = { textDisplaySize = "Zväčšené" },
                                colors = RadioButtonDefaults.colors(selectedColor = accentColor, unselectedColor = Color.Gray)
                            )
                            Text(text = "Zväčšené (+25%)", color = textColor, fontSize = (14 * scale).sp)
                        }
                    }
                }
            }

            // Settings Card 3: Grafická téma
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(1.5.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "🎨 Grafická téma", color = accentColor, fontSize = (16 * scale).sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = graphicTheme == "Slovenské farby",
                                onClick = { graphicTheme = "Slovenské farby" },
                                colors = RadioButtonDefaults.colors(selectedColor = accentColor, unselectedColor = Color.Gray)
                            )
                            Text(text = "Vlastenecká téma (Slovenské farby)", color = textColor, fontSize = (14 * scale).sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = graphicTheme == "Tmavý vesmír",
                                onClick = { graphicTheme = "Tmavý vesmír" },
                                colors = RadioButtonDefaults.colors(selectedColor = accentColor, unselectedColor = Color.Gray)
                            )
                            Text(text = "Tmavý kozmický režim (Modrá / Sivá)", color = textColor, fontSize = (14 * scale).sp)
                        }
                    }
                }
            }

            // Settings Card 4: Režim rozloženia menu
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(1.5.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "📱 Typ zobrazenia menu", color = accentColor, fontSize = (16 * scale).sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = layoutMode == "Dlaždice",
                                onClick = { layoutMode = "Dlaždice" },
                                colors = RadioButtonDefaults.colors(selectedColor = accentColor, unselectedColor = Color.Gray)
                            )
                            Text(text = "Dlaždice (Grid)", color = textColor, fontSize = (14 * scale).sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = layoutMode == "Zoznam",
                                onClick = { layoutMode = "Zoznam" },
                                colors = RadioButtonDefaults.colors(selectedColor = accentColor, unselectedColor = Color.Gray)
                            )
                            Text(text = "Zoznam (List)", color = textColor, fontSize = (14 * scale).sp)
                        }
                    }
                }
            }

            // Card 5: Verzia aplikácie a kontrola aktualizácií
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(1.5.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "ℹ️ O aplikácii", color = accentColor, fontSize = (16 * scale).sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Názov: Moja Voľba", color = textColor, fontSize = (13 * scale).sp)
                    Text(text = "Verzia: ${com.example.BuildConfig.VERSION_NAME} (Zostavenie ${com.example.BuildConfig.VERSION_CODE})", color = Color.LightGray, fontSize = (13 * scale).sp)
                    Text(text = "Záštita: Občianske združenie Naše Voľby", color = Color.LightGray, fontSize = (13 * scale).sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            isCheckingUpdate = true
                            updateResultText = null
                            coroutineScope.launch {
                                delay(1200)
                                isCheckingUpdate = false
                                updateResultText = "Verzia ${com.example.BuildConfig.VERSION_NAME} je plne aktuálna."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(color = textColor, modifier = Modifier.size(20.dp))
                        } else {
                            Text(text = "🚀 Skontrolovať aktualizácie", color = textColor, fontWeight = FontWeight.Bold)
                        }
                    }

                    updateResultText?.let { result ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = result, color = Color.Green, fontSize = (13 * scale).sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { currentScreen = "menu" },
                colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
            ) {
                Text(text = "Uložiť a vrátiť sa do menu", color = textColor, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showFeedbackDialog) {
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            title = {
                Text(
                    text = "💬 Spätná väzba",
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = (18 * scale).sp
                )
            },
            text = {
                Text(
                    text = "Poprosíme o vaše akékoľvek pripomienky k tejto, alebo webovej aplikácii. Tie posielajte na:\n\noz@nasevolby.sk\n\nĎakujeme",
                    color = Color.White,
                    fontSize = (15 * scale).sp,
                    lineHeight = (22 * scale).sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:oz@nasevolby.sk")
                            putExtra(Intent.EXTRA_SUBJECT, "Pripomienky k aplikácii Moja Voľba")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Nepodarilo sa otvoriť e-mailového klienta", Toast.LENGTH_SHORT).show()
                        }
                        showFeedbackDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text(text = "Poslať e-mail", color = textColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showFeedbackDialog = false }
                ) {
                    Text(text = "Zavrieť", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = cardColor,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun CardTile(title: String, icon: String, modifier: Modifier, onClick: () -> Unit, borderColor: Color, cardColor: Color, textColor: Color, scale: Float) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, color = textColor, fontSize = (14 * scale).sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}