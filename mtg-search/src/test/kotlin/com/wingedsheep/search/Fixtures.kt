package com.wingedsheep.search

/** Mirror of the frontend test fixture. Tiny but representative card set. */
data class TestCard(
    override val name: String,
    override val manaCost: String = "",
    override val cmc: Int = 0,
    override val colors: List<String> = emptyList(),
    override val colorIdentity: List<String> = emptyList(),
    override val cardTypes: List<String> = emptyList(),
    override val supertypes: List<String> = emptyList(),
    override val subtypes: List<String> = emptyList(),
    override val basicLand: Boolean = false,
    override val rarity: String = "COMMON",
    override val setCode: String? = null,
    override val collectorNumber: String? = null,
    override val oracleText: String? = null,
    override val power: String? = null,
    override val toughness: String? = null,
    override val keywords: List<String> = emptyList(),
    override val legalFormats: List<String> = emptyList(),
    override val isDoubleFaced: Boolean = false,
    override val printingSetCodes: List<String> = emptyList(),
    override val printedNames: List<String> = emptyList(),
) : SearchCard

object Fixtures {
    val CARDS: List<TestCard> = listOf(
        TestCard(
            name = "Lightning Bolt", manaCost = "{R}", cmc = 1,
            colors = listOf("RED"), colorIdentity = listOf("RED"),
            cardTypes = listOf("INSTANT"), rarity = "COMMON", setCode = "LEA",
            oracleText = "Lightning Bolt deals 3 damage to any target.",
            legalFormats = listOf("MODERN", "LEGACY", "VINTAGE"),
        ),
        TestCard(
            name = "Counterspell", manaCost = "{U}{U}", cmc = 2,
            colors = listOf("BLUE"), colorIdentity = listOf("BLUE"),
            cardTypes = listOf("INSTANT"), rarity = "COMMON", setCode = "LEA",
            oracleText = "Counter target spell.",
            legalFormats = listOf("LEGACY", "VINTAGE"),
        ),
        TestCard(
            name = "Llanowar Elves", manaCost = "{G}", cmc = 1,
            colors = listOf("GREEN"), colorIdentity = listOf("GREEN"),
            cardTypes = listOf("CREATURE"), subtypes = listOf("ELF", "DRUID"),
            rarity = "COMMON", setCode = "LEA",
            oracleText = "{T}: Add {G}.",
            power = "1", toughness = "1",
            legalFormats = listOf("MODERN", "LEGACY"),
        ),
        TestCard(
            name = "Serra Angel", manaCost = "{3}{W}{W}", cmc = 5,
            colors = listOf("WHITE"), colorIdentity = listOf("WHITE"),
            cardTypes = listOf("CREATURE"), subtypes = listOf("ANGEL"),
            rarity = "UNCOMMON", setCode = "LEA",
            oracleText = "Flying, vigilance.",
            power = "4", toughness = "4",
            keywords = listOf("FLYING", "VIGILANCE"),
            legalFormats = listOf("LEGACY"),
        ),
        TestCard(
            name = "Niv-Mizzet, Parun", manaCost = "{U}{U}{U}{R}{R}{R}", cmc = 6,
            colors = listOf("BLUE", "RED"), colorIdentity = listOf("BLUE", "RED"),
            cardTypes = listOf("CREATURE"), supertypes = listOf("LEGENDARY"),
            subtypes = listOf("DRAGON", "WIZARD"),
            rarity = "RARE", setCode = "GRN",
            oracleText = "Flying. Niv-Mizzet, Parun can't be countered.",
            power = "5", toughness = "5",
            keywords = listOf("FLYING"),
            legalFormats = listOf("MODERN", "LEGACY", "COMMANDER"),
        ),
        TestCard(
            name = "Forest", manaCost = "", cmc = 0,
            colors = emptyList(), colorIdentity = listOf("GREEN"),
            cardTypes = listOf("LAND"), supertypes = listOf("BASIC"),
            subtypes = listOf("FOREST"),
            basicLand = true, rarity = "COMMON", setCode = "LEA",
            oracleText = "({T}: Add {G}.)",
            legalFormats = listOf("MODERN", "LEGACY", "VINTAGE", "COMMANDER"),
        ),
        TestCard(
            name = "Tarmogoyf", manaCost = "{1}{G}", cmc = 2,
            colors = listOf("GREEN"), colorIdentity = listOf("GREEN"),
            cardTypes = listOf("CREATURE"), subtypes = listOf("LHURGOYF"),
            rarity = "MYTHIC", setCode = "FUT",
            oracleText = "Tarmogoyf's power is equal to the number of card types among cards in all graveyards and its toughness is equal to that number plus 1.",
            power = "*", toughness = "1+*",
            legalFormats = listOf("MODERN", "LEGACY"),
        ),
        // Planeswalker for `t:planeswalker loy=...` examples. We don't surface
        // loyalty as a numeric field yet, so loy comparisons are expected to
        // match nothing — but the parser must accept them without erroring.
        TestCard(
            name = "Jace, the Mind Sculptor", manaCost = "{2}{U}{U}", cmc = 4,
            colors = listOf("BLUE"), colorIdentity = listOf("BLUE"),
            cardTypes = listOf("PLANESWALKER"), supertypes = listOf("LEGENDARY"),
            subtypes = listOf("JACE"),
            rarity = "MYTHIC", setCode = "WWK",
            oracleText = "+2: Look at the top card of target player's library.",
            legalFormats = listOf("MODERN", "LEGACY", "VINTAGE"),
        ),
        // Two-color creature for boolean / parens examples.
        TestCard(
            name = "Lightning Helix", manaCost = "{R}{W}", cmc = 2,
            colors = listOf("RED", "WHITE"), colorIdentity = listOf("RED", "WHITE"),
            cardTypes = listOf("INSTANT"),
            rarity = "UNCOMMON", setCode = "RAV",
            oracleText = "Lightning Helix deals 3 damage to any target and you gain 3 life.",
            legalFormats = listOf("MODERN", "LEGACY"),
        ),
        // DFC for `is:dfc` / `layout:transform`.
        TestCard(
            name = "Delver of Secrets", manaCost = "{U}", cmc = 1,
            colors = listOf("BLUE"), colorIdentity = listOf("BLUE"),
            cardTypes = listOf("CREATURE"), subtypes = listOf("HUMAN", "WIZARD"),
            rarity = "COMMON", setCode = "ISD",
            oracleText = "At the beginning of your upkeep, look at the top card of your library.",
            power = "1", toughness = "1",
            legalFormats = listOf("MODERN", "LEGACY"),
            isDoubleFaced = true,
        ),
        // Reprint case for the `s:` matcher: canonical printing in BLB, reprinted in EOE.
        // `s:EOE` must surface this card via the reprint even though `setCode` is "BLB".
        TestCard(
            name = "Banishing Light", manaCost = "{2}{W}", cmc = 3,
            colors = listOf("WHITE"), colorIdentity = listOf("WHITE"),
            cardTypes = listOf("ENCHANTMENT"),
            rarity = "UNCOMMON", setCode = "BLB",
            oracleText = "When Banishing Light enters, exile target nonland permanent an opponent controls until Banishing Light leaves the battlefield.",
            legalFormats = listOf("STANDARD", "MODERN"),
            printingSetCodes = listOf("BLB", "EOE"),
        ),
    )

    /** Printed-name case, kept out of [CARDS]: Through the Omenpaths prints it as "Ademi of the Silkchutes". */
    val SPECTACULAR_SPIDER_MAN = TestCard(
        name = "Spectacular Spider-Man", manaCost = "{1}{W}", cmc = 2,
        colors = listOf("WHITE"), colorIdentity = listOf("WHITE"),
        cardTypes = listOf("CREATURE"), supertypes = listOf("LEGENDARY"),
        rarity = "RARE", setCode = "SPM",
        power = "3", toughness = "2",
        printingSetCodes = listOf("SPM", "OM1"),
        printedNames = listOf("Ademi of the Silkchutes"),
    )
}
