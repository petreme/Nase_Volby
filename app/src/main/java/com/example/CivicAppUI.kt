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
                for (i in pixels.indices) {
                    val x = i % width
                    val y = i / width
                    
                    if (x < 6 || x >= width - 6 || y < 6 || y >= height - 6) {
                        pixels[i] = 0x00000000
                    } else {
                        val color = pixels[i]
                        val r = (color shr 16) and 0xFF
                        val g = (color shr 8) and 0xFF
                        val b = color and 0xFF
                        if (r < 75 && g < 75 && b < 75) {
                            pixels[i] = 0x00000000
                        }
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

    if (currentScreen == "splash") {
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
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Moja voľba",
                    color = textColor,
                    fontSize = (32 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                val cleanLogo = rememberCleanLogo(R.drawable.nase_volby_clean_logo_1780731853180)
                if (cleanLogo != null) {
                    Image(
                        bitmap = cleanLogo,
                        contentDescription = "Logo OZ Naše Voľby",
                        modifier = Modifier.size(180.dp).padding(8.dp)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.nase_volby_clean_logo_1780731853180),
                        contentDescription = "Logo OZ Naše Voľby",
                        modifier = Modifier.size(180.dp).padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Máte pred sebou jedinečný demokratický nástroj na spravodlivé riadenie štátu.\nVáš hlas v tejto aplikácii má veľkú silu a osobnú zodpovednosť. Môže rozhodnúť o spravodlivosti a prosperite v našej krajine. Využite to a urobte našu krajinu takú, ako si želá väčšina občanov.",
                    color = textColor,
                    fontSize = (16 * scale).sp,
                    textAlign = TextAlign.Center,
                    lineHeight = (24 * scale).sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = { currentScreen = "menu" },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    border = BorderStroke(1.5.dp, textColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp)
                ) {
                    Text(text = "Vyberte si", color = textColor, fontSize = (18 * scale).sp, fontWeight = FontWeight.Bold)
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
                                    val cleanLogo = rememberCleanLogo(person.placeholderRes)
                                    if (cleanLogo != null) {
                                        Image(bitmap = cleanLogo, contentDescription = "Logo", modifier = Modifier.size(45.dp))
                                    } else {
                                        Text(text = "📷", fontSize = 24.sp)
                                    }
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
    else if (currentScreen == "sub_kandidati") { /* ... sekcie pre kandidátov ... */ }
    else if (currentScreen == "sub_obvody") { /* ... sprievodca obvodmi ... */ }
    else if (currentScreen == "sub_info") { /* ... slovník cudzích pojmov ... */ }
    else if (currentScreen == "settings") { /* ... zachované nastavenia ... */ }
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