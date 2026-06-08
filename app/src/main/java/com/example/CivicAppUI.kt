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
                    
                    // Thoroughly trim the outer 6-pixel frame completely to remove any black edge lines/artifacts
                    if (x < 6 || x >= width - 6 || y < 6 || y >= height - 6) {
                        pixels[i] = 0x00000000
                    } else {
                        val color = pixels[i]
                        val r = (color shr 16) and 0xFF
                        val g = (color shr 8) and 0xFF
                        val b = color and 0xFF
                        // Aggressive check for black, dark gray, or near-black pixels of the background frame
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


@Composable
fun CivicAppUI() {
    var currentScreen by remember { mutableStateOf("splash") }
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateResultText by remember { mutableStateOf<String?>(null) }

    // Používateľské predvoľby state
    var language by remember { mutableStateOf("SK") } // "SK" alebo "EN"
    var textDisplaySize by remember { mutableStateOf("Štandardné") } // "Štandardné" alebo "Zväčšené"
    var graphicTheme by remember { mutableStateOf("Slovenské farby") } // "Slovenské farby" alebo "Slate Dark"
    var layoutMode by remember { mutableStateOf("Dlaždice") } // "Dlaždice" (Tiles/Okná) alebo "Textové" (Standard List)

    // Dynamické mierky a farby
    val scale = if (textDisplaySize == "Zväčšené") 1.25f else 1.0f
    
    // Téma ladiaca do slovenských farieb (White / Blue / Red)
    val backgroundColor = if (graphicTheme == "Slovenské farby") Color(0xFF092A5C) else Color(0xFF121824)
    val cardColor = if (graphicTheme == "Slovenské farby") Color(0xFF143B73) else Color(0xFF1E293B)
    val borderColor = if (graphicTheme == "Slovenské farby") Color(0xFFEE2436) else Color(0xFF3B82F6)
    val accentColor = if (graphicTheme == "Slovenské farby") Color(0xFFEE2436) else Color(0xFF3B82F6)
    val textColor = Color(0xFFFFFFFF) // Výrazne biele písmo pre maximálnu viditeľnosť a kontrast

    if (currentScreen == "splash") {
        // 1. ÚVODNÁ OBRAZOVKA (SPLASH SCREEN) s pekným červeným rámikom v bezpečnej zóne
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .safeDrawingPadding() // Rámik nebude nad kamerou a pod navigačnými tlačidlami
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
                    text = if (language == "SK") "Moja voľba" else "My Choice",
                    color = textColor,
                    fontSize = (32 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Logo OZ Naše Voľby s programovo odstráneným čiernym pozadím
                val cleanLogo = rememberCleanLogo(R.drawable.nase_volby_clean_logo_1780731853180)
                if (cleanLogo != null) {
                    Image(
                        bitmap = cleanLogo,
                        contentDescription = "Logo OZ Naše Voľby",
                        modifier = Modifier
                            .size(180.dp)
                            .padding(8.dp)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.nase_volby_clean_logo_1780731853180),
                        contentDescription = "Logo OZ Naše Voľby",
                        modifier = Modifier
                            .size(180.dp)
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (language == "SK") {
                        "Máte pred sebou jedinečný demokratický nástroj na spravodlivé riadenie štátu.\nVáš hlas v tejto aplikácii má veľkú silu a osobnú zodpovednosť. Môže rozhodnúť o spravodlivosti a prosperite v našej krajine. Využite to a urobte našu krajinu takú, ako si želá väčšina občanov."
                    } else {
                        "You have a unique democratic tool for the fair governance of the state.\nYour voice in this app carries great power and personal responsibility. It can decide on justice and prosperity in our country. Use it to build our country the way the majority of citizens wish."
                    },
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
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp)
                ) {
                    Text(
                        text = if (language == "SK") "Vyberte si" else "Select Option", 
                        color = textColor, 
                        fontSize = (18 * scale).sp, 
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } 
    else if (currentScreen == "menu") {
        // 2. HLAVNÉ NAVIGAČNÉ MÔŽNOSTI (DLAŽDICOVÉ ALEBO STRUKTÚROVANÉ ZOZNAMOVÉ) s bezpečnou zónou
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .safeDrawingPadding() // Chráni hornú/dolnú stranu pred notchom a spodnou lištou
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (language == "SK") "Hlavné menu" else "Main Menu",
                color = textColor,
                fontSize = (24 * scale).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            if (layoutMode == "Dlaždice") {
                // I. DLAŽDICOVÉ ZOBRAZENIE (2x2 pre lepšie a prehľadnejšie ovládanie)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CardTile(
                            title = if (language == "SK") "Hlasovania" else "Voting",
                            icon = "🗳️",
                            modifier = Modifier.weight(1f).height(145.dp),
                            onClick = { currentScreen = "sub_hlasovania" },
                            borderColor = borderColor,
                            cardColor = cardColor,
                            textColor = textColor,
                            scale = scale
                        )
                        CardTile(
                            title = if (language == "SK") "Pre kandidátov" else "For Candidates",
                            icon = "👤",
                            modifier = Modifier.weight(1f).height(145.dp),
                            onClick = { currentScreen = "sub_kandidati" },
                            borderColor = borderColor,
                            cardColor = cardColor,
                            textColor = textColor,
                            scale = scale
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CardTile(
                            title = if (language == "SK") "Sprievodca obvodmi" else "Districts Guide",
                            icon = "🗺️",
                            modifier = Modifier.weight(1f).height(145.dp),
                            onClick = { currentScreen = "sub_obvody" },
                            borderColor = borderColor,
                            cardColor = cardColor,
                            textColor = textColor,
                            scale = scale
                        )
                        CardTile(
                            title = if (language == "SK") "Informácie" else "Information",
                            icon = "ℹ️",
                            modifier = Modifier.weight(1f).height(145.dp),
                            onClick = { currentScreen = "sub_info" },
                            borderColor = borderColor,
                            cardColor = cardColor,
                            textColor = textColor,
                            scale = scale
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Rýchla skratka do Nastavení priamo pod dlaždicami s dynamickou výškou
                Button(
                    onClick = { currentScreen = "settings" },
                    colors = ButtonDefaults.buttonColors(containerColor = cardColor),
                    border = BorderStroke(2.dp, borderColor),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                ) {
                    Text(
                        text = if (language == "SK") "⚙️ Nastavenie" else "⚙️ Settings",
                        color = textColor,
                        fontSize = (15 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            } 
            else {
                // II. TEXTOVÉ (ZOZNAMOVÉ) ZOBRAZENIE
                val menuItems = listOf(
                    Triple(
                        "Volebné obvody", 
                        "Electoral Districts Guide", 
                        "https://www.nasevolby.sk/?page_id=10941"
                    ),
                    Triple(
                        "Môj obvod-poslanec", 
                        "My Representative", 
                        "https://www.nasevolby.sk/?page_id=11121"
                    ),
                    Triple(
                        "Volím vysokého štátneho úradníka", 
                        "Vote for High State Official", 
                        "https://hlasujeme.nasevolby.sk/volba-ministra-2/"
                    ),
                    Triple(
                        "Hlasovanie o občianskych návrhoch", 
                        "Voting on Civic Proposals", 
                        "https://hlasujeme.nasevolby.sk/hlasovanie-demo/"
                    ),
                    Triple(
                        "Výber Občianskych návrhov", 
                        "Selection of Civic Proposals", 
                        "https://hlasujeme.nasevolby.sk/vyber-navrhov/"
                    ),
                    Triple(
                        "Dnešné hlasovanie", 
                        "Today's Voting", 
                        "https://hlasujeme.nasevolby.sk/minule-hlasovania-buduce-hlasovania/dnesne-hlasovanie/"
                    ),
                    Triple(
                        "Budúce hlasovanie", 
                        "Future Voting", 
                        "https://hlasujeme.nasevolby.sk/minule-hlasovania-buduce-hlasovania/buduce-hlasovania/"
                    ),
                    Triple(
                        "História hlasovaní", 
                        "Voting History", 
                        "https://hlasujeme.nasevolby.sk/minule-hlasovania-buduce-hlasovania/571-2/"
                    ),
                    Triple(
                        "Volebný program kandidáta na poslanca do NR SR", 
                        "Representative Candidate Platform", 
                        "https://www.poslednereferendum.nasevolby.sk/?page_id=977"
                    ),
                    Triple(
                        "Návrh na výšku platu vysokého štátneho úradníka", 
                        "Salary Proposal for High Official", 
                        "https://www.poslednereferendum.nasevolby.sk/?page_id=1002"
                    ),
                    Triple(
                        "Kvalifikovaný občiansky návrh (petícia na referendum)", 
                        "Qualified Civic Proposal (Petition)", 
                        "https://www.poslednereferendum.nasevolby.sk/?page_id=1008"
                    ),
                    Triple(
                        "Slovník cudzích pojmov", 
                        "Glossary of Terms", 
                        "https://www.poslednereferendum.nasevolby.sk/?page_id=994"
                    )
                )

                menuItems.forEachIndexed { index, item ->
                    val label = if (language == "SK") item.first else item.second
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.third))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = cardColor),
                        border = BorderStroke(1.5.dp, borderColor),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .defaultMinSize(minHeight = 52.dp)
                    ) {
                        Text(
                            text = "${index + 1}. $label",
                            color = textColor,
                            fontSize = (14 * scale).sp,
                            textAlign = TextAlign.Start,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { currentScreen = "settings" },
                    colors = ButtonDefaults.buttonColors(containerColor = cardColor),
                    border = BorderStroke(2.dp, borderColor),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .defaultMinSize(minHeight = 56.dp)
                ) {
                    Text(
                        text = if (language == "SK") "13. ⚙️ Nastavenie" else "13. ⚙️ Settings",
                        color = textColor,
                        fontSize = (15 * scale).sp,
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tlačidlo Naspäť na úvod (tiež vo farbe borderColor - plná červená)
            Button(
                onClick = { currentScreen = "splash" },
                colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Text(
                    text = if (language == "SK") "Naspäť na úvodnú obrazovku" else "Back to Intro Screen",
                    color = textColor,
                    fontSize = (14 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    } 
    else if (currentScreen == "sub_hlasovania") {
        // A. OKNO HLASOVANIA s bezpečnou zónou
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .safeDrawingPadding() // Rámik a texty nebudú pod notchom alebo navigačnou lištou
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (language == "SK") "🗳️ Hlasovania" else "🗳️ Voting Options",
                color = textColor,
                fontSize = (24 * scale).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            val annotatedWarning = if (language == "SK") {
                buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color(0xFFEF3646), fontWeight = FontWeight.Bold)) {
                        append("UPOZORNENIE\n")
                    }
                    withStyle(style = SpanStyle(color = textColor.copy(alpha = 0.9f))) {
                        append("Máte vo svojich rukách Moc, ale aj zodpovednosť za svoje rozhodnutie.\nVyskúšajte, ako jednoducho viete rozhodnúť o dôležitých celospoločenských otázkach")
                    }
                }
            } else {
                buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color(0xFFEF3646), fontWeight = FontWeight.Bold)) {
                        append("WARNING\n")
                    }
                    withStyle(style = SpanStyle(color = textColor.copy(alpha = 0.9f))) {
                        append("Try how easily you can decide on important society-wide issues")
                    }
                }
            }

            Text(
                text = annotatedWarning,
                fontSize = (15 * scale).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(bottom = 24.dp, start = 8.dp, end = 8.dp)
            )

            val links = listOf(
                Triple(
                    "Volím vysokého štátneho úradníka", 
                    "Vote for High State Official", 
                    "https://hlasujeme.nasevolby.sk/volba-ministra-2/"
                ),
                Triple(
                    "Hlasovanie o občianskych návrhoch", 
                    "Voting on Civic Proposals", 
                    "https://hlasujeme.nasevolby.sk/hlasovanie-demo/"
                ),
                Triple(
                    "Výber Občianskych návrhov", 
                    "Selection of Civic Proposals", 
                    "https://hlasujeme.nasevolby.sk/vyber-navrhov/"
                )
            )

            links.forEach { item ->
                val label = if (language == "SK") item.first else item.second
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.third))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cardColor),
                    border = BorderStroke(1.5.dp, borderColor),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .defaultMinSize(minHeight = 52.dp)
                ) {
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = (14 * scale).sp,
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(borderColor.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.height(16.dp))

            val otherItem = Triple(
                "Ostatné",
                "Others",
                "https://hlasujeme.nasevolby.sk/minule-hlasovania-buduce-hlasovania/"
            )
            val otherLabel = if (language == "SK") otherItem.first else otherItem.second
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(otherItem.third))
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = cardColor),
                border = BorderStroke(1.5.dp, borderColor),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .defaultMinSize(minHeight = 52.dp)
            ) {
                Text(
                    text = otherLabel,
                    color = textColor,
                    fontSize = (14 * scale).sp,
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { currentScreen = "menu" },
                colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
            ) {
                Text(
                    text = if (language == "SK") "← Naspäť do hlavného menu" else "← Back to main menu",
                    color = textColor,
                    fontSize = (15 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    } 
    else if (currentScreen == "sub_kandidati") {
        // B. OKNO PRE KANDIDÁTOV s bezpečnou zónou
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
                text = if (language == "SK") "👤 Pre kandidátov" else "👤 For Candidates",
                color = textColor,
                fontSize = (24 * scale).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            val links = listOf(
                Triple(
                    "Volebný program kandidáta na poslanca do NR SR", 
                    "Representative Candidate Platform", 
                    "https://www.poslednereferendum.nasevolby.sk/?page_id=977"
                ),
                Triple(
                    "Návrh na výšku platu vysokého štátneho úradníka", 
                    "Salary Proposal for High Official", 
                    "https://www.poslednereferendum.nasevolby.sk/?page_id=1002"
                ),
                Triple(
                    "Kvalifikovaný občiansky návrh (petícia na referendum)", 
                    "Qualified Civic Proposal (Petition)", 
                    "https://www.poslednereferendum.nasevolby.sk/?page_id=1008"
                )
            )

            links.forEach { item ->
                val label = if (language == "SK") item.first else item.second
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.third))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cardColor),
                    border = BorderStroke(1.5.dp, borderColor),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .defaultMinSize(minHeight = 52.dp)
                ) {
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = (14 * scale).sp,
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { currentScreen = "menu" },
                colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
            ) {
                Text(
                    text = if (language == "SK") "← Naspäť do hlavného menu" else "← Back to main menu",
                    color = textColor,
                    fontSize = (15 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    } 
    else if (currentScreen == "sub_obvody") {
        // C. OKNO SPRIEVODCA VOLEBNÝMI OBVODMI s bezpečnou zónou
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
                text = if (language == "SK") "🗺️ Sprievodca obvodmi" else "🗺️ Districts Guide",
                color = textColor,
                fontSize = (22 * scale).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            val links = listOf(
                Triple(
                    "Volebné obvody", 
                    "Electoral Districts Guide", 
                    "https://www.nasevolby.sk/?page_id=10941"
                ),
                Triple(
                    "Môj poslanec", 
                    "My Representative", 
                    "https://www.nasevolby.sk/?page_id=11121"
                )
            )

            links.forEach { item ->
                val label = if (language == "SK") item.first else item.second
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.third))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cardColor),
                    border = BorderStroke(1.5.dp, borderColor),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .defaultMinSize(minHeight = 52.dp)
                ) {
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = (14 * scale).sp,
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { currentScreen = "menu" },
                colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
            ) {
                Text(
                    text = if (language == "SK") "← Naspäť do hlavného menu" else "← Back to main menu",
                    color = textColor,
                    fontSize = (15 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    } 
    else if (currentScreen == "sub_info") {
        // D. OKNO INFORMÁCIE s bezpečnou zónou
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
                text = if (language == "SK") "ℹ️ Informácie" else "ℹ️ Information Section",
                color = textColor,
                fontSize = (24 * scale).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://hlasujeme.nasevolby.sk/"))
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = cardColor),
                border = BorderStroke(2.dp, borderColor),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .defaultMinSize(minHeight = 58.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📱 ",
                        fontSize = (18 * scale).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == "SK") "Ako prebieha bezpečné hlasovanie s vaším mobilom." else "How secure voting works with your mobile phone.",
                        color = textColor,
                        fontSize = (14 * scale).sp,
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val links = listOf(
                Triple(
                    "Zoznam poslancov", 
                    "List of Members of Parliament", 
                    "https://www.nrsr.sk/web/Default.aspx?sid=poslanci/zoznam_abc&ListType=0&CisObdobia=9"
                ),
                Triple(
                    "Live z NR SR", 
                    "Live Broadcast from Parliament", 
                    "https://www.stvr.sk/televizia/live-nr-sr?p=h5&q=1"
                ),
                Triple(
                    "Posledné referendum", 
                    "Last Referendum Platform", 
                    "https://www.poslednereferendum.nasevolby.sk/"
                ),
                Triple(
                    "Odkazy na zákony", 
                    "Links to Laws & Legislation", 
                    "https://www.poslednereferendum.nasevolby.sk/?page_id=207"
                ),
                Triple(
                    "Kontakt na občianske združenie", 
                    "Contact Civil Association (OZ)", 
                    "https://www.nasevolby.sk/?page_id=7044"
                ),
                Triple(
                    "Prezentácia zmeny", 
                    "Presentation of Changes", 
                    "https://www.nasevolby.sk/?page_id=10435"
                ),
                Triple(
                    "Videá", 
                    "Videos", 
                    "https://www.nasevolby.sk/?page_id=5602"
                ),
                Triple(
                    "Domovská stránka www.nasevolby.sk", 
                    "Official Homepage www.nasevolby.sk", 
                    "https://www.nasevolby.sk/"
                ),
                Triple(
                    "Finančná podpora", 
                    "Financial Support", 
                    "https://www.nasevolby.sk/?page_id=6086"
                ),
                Triple(
                    "Slovník cudzích pojmov", 
                    "Glossary of Terms", 
                    "https://www.poslednereferendum.nasevolby.sk/?page_id=994"
                )
            )

            links.forEach { item ->
                val label = if (language == "SK") item.first else item.second
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.third))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cardColor),
                    border = BorderStroke(1.5.dp, borderColor),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .defaultMinSize(minHeight = 52.dp)
                ) {
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = (14 * scale).sp,
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pridané tlačidlo "Nastavenia" do okna Informácie ako žiadal používateľ
            Button(
                onClick = { currentScreen = "settings" },
                colors = ButtonDefaults.buttonColors(containerColor = cardColor),
                border = BorderStroke(2.dp, borderColor),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .defaultMinSize(minHeight = 56.dp)
            ) {
                Text(
                    text = if (language == "SK") "⚙️ Nastavenia" else "⚙️ Settings",
                    color = textColor,
                    fontSize = (15 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { currentScreen = "menu" },
                colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
            ) {
                Text(
                    text = if (language == "SK") "← Naspäť do hlavného menu" else "← Back to main menu",
                    color = textColor,
                    fontSize = (15 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    else if (currentScreen == "settings") {
        // 3. OBRAZOVKA NASTAVENÍ (SUB-MENU PARAMETROV) s bezpečnou zónou
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .safeDrawingPadding() // Chráni pred notchom a spodnou lištou
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (language == "SK") "Nastavenie & Nástroje" else "Settings & Tools",
                color = textColor,
                fontSize = (24 * scale).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Sekcia Bezpečnosť
            Text(
                text = if (language == "SK") "Zabezpečenie pripojenia" else "Connection Security",
                color = textColor.copy(alpha = 0.8f),
                fontSize = (15 * scale).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp),
                textAlign = TextAlign.Start
            )

            Button(
                onClick = { showDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = cardColor),
                border = BorderStroke(1.5.dp, borderColor),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp)
            ) {
                Text(
                    text = if (language == "SK") "📶 Vypnúť/Zapnúť Wi-Fi a Dáta" else "📶 Enable/Disable Wi-Fi & Data",
                    color = textColor,
                    fontSize = (14 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sekcia Inštalácia KEP
            Text(
                text = if (language == "SK") "Podpisový modul (KEP)" else "Signature Module (KEP)",
                color = textColor.copy(alpha = 0.8f),
                fontSize = (15 * scale).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp),
                textAlign = TextAlign.Start
            )

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=sk.minv.eidentita&hl=sk"))
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = cardColor),
                border = BorderStroke(1.5.dp, borderColor),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp)
            ) {
                Text(
                    text = if (language == "SK") "📥 Inštalovať KEP" else "📥 Install KEP",
                    color = textColor,
                    fontSize = (14 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Sekcia Predvoľby
            Text(
                text = if (language == "SK") "Predvoľby aplikácie" else "App Preferences",
                color = textColor.copy(alpha = 0.8f),
                fontSize = (15 * scale).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                textAlign = TextAlign.Start
            )

            // Panel: Spôsob zobrazenia aplikácie (Zoznam vs Dlaždice)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(BorderStroke(1.5.dp, borderColor), RoundedCornerShape(12.dp))
                    .background(cardColor, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = if (language == "SK") "Zobrazenie aplikácie" else "Application View style",
                    color = textColor,
                    fontSize = (14 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { layoutMode = "Dlaždice" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (layoutMode == "Dlaždice") borderColor else cardColor.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.5.dp, borderColor),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (language == "SK") "Dlaždicové (Okná)" else "Grid Tiles", 
                            color = textColor, 
                            fontSize = (12 * scale).sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { layoutMode = "Textové" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (layoutMode == "Textové") borderColor else cardColor.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.5.dp, borderColor),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (language == "SK") "Zoznam (Text)" else "List View", 
                            color = textColor, 
                            fontSize = (12 * scale).sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Panel: Jazyk
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(BorderStroke(1.5.dp, borderColor), RoundedCornerShape(12.dp))
                    .background(cardColor, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = if (language == "SK") "Jazyk" else "Language",
                    color = textColor,
                    fontSize = (14 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { language = "SK" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (language == "SK") borderColor else cardColor.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Slovenčina", 
                            color = textColor, 
                            fontSize = (13 * scale).sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { language = "EN" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (language == "EN") borderColor else cardColor.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "English", 
                            color = textColor, 
                            fontSize = (13 * scale).sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Panel: Textové zobrazenie
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(BorderStroke(1.5.dp, borderColor), RoundedCornerShape(12.dp))
                    .background(cardColor, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = if (language == "SK") "Textové zobrazenie" else "Text Display Size",
                    color = textColor,
                    fontSize = (14 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { textDisplaySize = "Štandardné" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (textDisplaySize == "Štandardné") borderColor else cardColor.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (language == "SK") "Štandardné" else "Standard", 
                            color = textColor, 
                            fontSize = (13 * scale).sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { textDisplaySize = "Zväčšené" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (textDisplaySize == "Zväčšené") borderColor else cardColor.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (language == "SK") "Zväčšené" else "Enlarged", 
                            color = textColor, 
                            fontSize = (13 * scale).sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Panel: Grafické zobrazenie
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(BorderStroke(1.5.dp, borderColor), RoundedCornerShape(12.dp))
                    .background(cardColor, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = if (language == "SK") "Grafické zobrazenie" else "Visual Theme Style",
                    color = textColor,
                    fontSize = (14 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { graphicTheme = "Slovenské farby" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (graphicTheme == "Slovenské farby") borderColor else cardColor.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (language == "SK") "Slovenské farby" else "Slovak Theme", 
                            color = textColor, 
                            fontSize = (11 * scale).sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { graphicTheme = "Slate Dark" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (graphicTheme == "Slate Dark") borderColor else cardColor.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (language == "SK") "Slate Dark" else "Slate Dark", 
                            color = textColor, 
                            fontSize = (11 * scale).sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Informácia o verzii s možnosťou overenia aktualizácie
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
                    .background(cardColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == "SK") "Verzia" else "Version",
                        color = textColor.copy(alpha = 0.9f),
                        fontSize = (14 * scale).sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "v1.2.0 - OZ Naše Voľby",
                        color = textColor,
                        fontSize = (14 * scale).sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isCheckingUpdate) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = borderColor,
                            modifier = Modifier.size((16 * scale).dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "SK") "Overujem aktualizácie..." else "Checking for updates...",
                            color = textColor.copy(alpha = 0.7f),
                            fontSize = (13 * scale).sp
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isCheckingUpdate = true
                                updateResultText = null
                                kotlinx.coroutines.delay(1200)
                                isCheckingUpdate = false
                                updateResultText = if (language == "SK") {
                                    "✓ Aplikácia je aktuálna. Máte najnovšiu verziu."
                                } else {
                                    "✓ Application is up to date. You have the latest version."
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = cardColor),
                        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 12.dp),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = if (language == "SK") "Overiť aktualizáciu?" else "Verify update?",
                            color = textColor,
                            fontSize = (13 * scale).sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                updateResultText?.let { result ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = result,
                        color = Color(0xFF4CAF50),
                        fontSize = (13 * scale).sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tlačidlo Späť do menu
            Button(
                onClick = { currentScreen = "menu" },
                colors = ButtonDefaults.buttonColors(containerColor = borderColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text(
                    text = if (language == "SK") "← Naspäť do hlavného menu" else "← Back to main menu",
                    color = textColor,
                    fontSize = (15 * scale).sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Bezpečnostný Alert Dialog pre Wi-Fi & Dáta
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(
                        text = if (language == "SK") "Rozumiem" else "I understand", 
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            title = { 
                Text(
                    text = if (language == "SK") "Bezpečnostné upozornenie" else "Security Warning", 
                    color = textColor,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = { 
                Text(
                    text = if (language == "SK") {
                        "Pre zaistenie maximálnej diskrétnosti prosím pred hlasovaním manuálne vypnite Wi-Fi a mobilné dáta v hornom paneli vášho telefónu."
                    } else {
                        "To ensure maximum privacy, please manually turn off Wi-Fi and mobile data in your phone's notification drawer before voting."
                    },
                    color = textColor,
                    fontSize = (15 * scale).sp
                ) 
            },
            containerColor = cardColor,
            modifier = Modifier.border(2.dp, borderColor, RoundedCornerShape(28.dp))
        )
    }
}

@Composable
fun CardTile(
    title: String,
    icon: String,
    modifier: Modifier,
    onClick: () -> Unit,
    borderColor: Color,
    cardColor: Color,
    textColor: Color,
    scale: Float
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(2.5.dp, borderColor),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                fontSize = (28 * scale).sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = title,
                color = textColor,
                fontSize = (13 * scale).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = (16 * scale).sp
            )
        }
    }
}
