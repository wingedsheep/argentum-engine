package com.wingedsheep.sdk.core

import com.wingedsheep.sdk.model.Rarity

/**
 * Primordial (<https://primordialformat.com/rules>, rules version 4.00) — a fan-made, set-based
 * constructed format. Every card in a deck comes from **one** block or standalone set, and the
 * deck has a fixed rarity budget:
 *
 *  - at least [MIN_DECK_SIZE] cards;
 *  - exactly [RARE_SLOTS] rare or mythic cards, at most one of each;
 *  - exactly [UNCOMMON_SLOTS] uncommon cards, at most two of each;
 *  - any number of commons, at most four of each;
 *  - any number of the five non-snow basic lands ([FREE_BASIC_LANDS]), whatever the set;
 *  - a sideboard of at most [MAX_SIDEBOARD] cards from the same block or set. The per-name copy
 *    limits count deck and sideboard together; the exact rare/uncommon totals bind the main deck
 *    only.
 *
 * A card's rarity is its **lowest** printed rarity within the chosen block or set (rules update
 * 4.00), so a card printed common in one set of a block and uncommon in another is a common there.
 *
 * Pure data, like [CanadianHighlanderPoints]: the deck validator, the deckbuilder and the AI deck
 * generator all read the format from here. Which cards a block holds is not listed here — that is
 * read off the set definitions' printings, so a newly implemented set's cards join its pool on
 * their own.
 */
object PrimordialFormat {

    const val MIN_DECK_SIZE = 40
    const val RARE_SLOTS = 2
    const val UNCOMMON_SLOTS = 6
    const val MAX_SIDEBOARD = 10

    /** "Plains, Island, Swamp, Mountain and Forest can be played with any set" — snow-covered ones can't. */
    val FREE_BASIC_LANDS: Set<String> = setOf("Plains", "Island", "Swamp", "Mountain", "Forest")

    /** The three rarity budgets a Primordial deck is built from. */
    enum class Slot(val label: String, val maxCopies: Int) {
        RARE("Rare or mythic", 1),
        UNCOMMON("Uncommon", 2),
        COMMON("Common", 4);

        companion object {
            /**
             * The budget a printed [rarity] draws from. Special and bonus rarities (no Primordial
             * pool prints one today) are treated as rares — the strictest slot — rather than
             * slipping through as commons.
             */
            fun of(rarity: Rarity): Slot = when (rarity) {
                Rarity.COMMON -> COMMON
                Rarity.UNCOMMON -> UNCOMMON
                Rarity.RARE, Rarity.MYTHIC, Rarity.SPECIAL, Rarity.BONUS -> RARE
            }
        }
    }

    /**
     * One legal block or standalone set: the unit a Primordial deck is built from.
     *
     * @property name What players call it ("Zendikar block", "Bloomburrow").
     * @property setCodes Upper-case set codes it spans, earliest first.
     * @property playable False for the two sets the rules flag as unplayable because they print no
     *   commons (The Big Score, March of the Machine: The Aftermath). They stay legal; nothing
     *   auto-builds from them.
     */
    data class Pool(val name: String, val setCodes: List<String>, val playable: Boolean = true) {
        val isBlock: Boolean get() = setCodes.size > 1
    }

    private fun set(name: String, code: String, playable: Boolean = true) = Pool(name, listOf(code), playable)
    private fun block(name: String, vararg codes: String) = Pool("$name block", codes.toList())

    /**
     * Every legal block and standalone set, newest first — the order the rules page lists them
     * and the order a tie between two candidate pools is broken in. Bonus sheets and companion
     * products (Mystical Archive, Kaladesh Inventions, Zendikar Expeditions, Amonkhet Invocations,
     * Commander decks, Masters sets, Modern Horizons, Portal and Starter sets) are not legal and
     * are not listed.
     */
    val pools: List<Pool> = listOf(
        set("Secrets of Strixhaven", "SOS"),
        set("Teenage Mutant Ninja Turtles", "TMT"),
        set("Lorwyn Eclipsed", "ECL"),
        set("Avatar: The Last Airbender", "TLA"),
        set("Marvel's Spider-Man", "SPM"),
        set("Edge of Eternities", "EOE"),
        set("Final Fantasy", "FIN"),
        set("Tarkir: Dragonstorm", "TDM"),
        set("Aetherdrift", "DFT"),
        set("Foundations", "FDN"),
        set("Duskmourn: House of Horror", "DSK"),
        set("Bloomburrow", "BLB"),
        set("The Big Score", "BIG", playable = false),
        set("Outlaws of Thunder Junction", "OTJ"),
        set("Murders at Karlov Manor", "MKM"),
        set("The Lost Caverns of Ixalan", "LCI"),
        set("Wilds of Eldraine", "WOE"),
        set("March of the Machine: The Aftermath", "MAT", playable = false),
        set("March of the Machine", "MOM"),
        set("Phyrexia: All Will Be One", "ONE"),
        set("The Brothers' War", "BRO"),
        set("Dominaria United", "DMU"),
        set("Streets of New Capenna", "SNC"),
        set("Kamigawa: Neon Dynasty", "NEO"),
        set("Innistrad: Crimson Vow", "VOW"),
        set("Innistrad: Midnight Hunt", "MID"),
        set("Adventures in the Forgotten Realms", "AFR"),
        set("Strixhaven: School of Mages", "STX"),
        set("Kaldheim", "KHM"),
        set("Zendikar Rising", "ZNR"),
        set("Core Set 2021", "M21"),
        set("Ikoria: Lair of Behemoths", "IKO"),
        set("Theros Beyond Death", "THB"),
        set("Throne of Eldraine", "ELD"),
        set("Core Set 2020", "M20"),
        set("War of the Spark", "WAR"),
        set("Ravnica Allegiance", "RNA"),
        set("Guilds of Ravnica", "GRN"),
        set("Core Set 2019", "M19"),
        set("Dominaria", "DOM"),
        block("Ixalan", "XLN", "RIX"),
        block("Amonkhet", "AKH", "HOU"),
        block("Kaladesh", "KLD", "AER"),
        block("Shadows over Innistrad", "SOI", "EMN"),
        block("Battle for Zendikar", "BFZ", "OGW"),
        set("Magic Origins", "ORI"),
        block("Khans of Tarkir", "KTK", "FRF", "DTK"),
        set("Magic 2015", "M15"),
        block("Theros", "THS", "BNG", "JOU"),
        set("Magic 2014", "M14"),
        block("Return to Ravnica", "RTR", "GTC", "DGM"),
        set("Magic 2013", "M13"),
        block("Innistrad", "ISD", "DKA", "AVR"),
        set("Magic 2012", "M12"),
        block("Scars of Mirrodin", "SOM", "MBS", "NPH"),
        set("Magic 2011", "M11"),
        block("Zendikar", "ZEN", "WWK", "ROE"),
        set("Magic 2010", "M10"),
        block("Shards of Alara", "ALA", "CON", "ARB"),
        block("Shadowmoor", "SHM", "EVE"),
        block("Lorwyn", "LRW", "MOR"),
        set("Tenth Edition", "10E"),
        block("Time Spiral", "TSP", "TSB", "PLC", "FUT"),
        block("Ravnica", "RAV", "GPT", "DIS"),
        set("Ninth Edition", "9ED"),
        block("Kamigawa", "CHK", "BOK", "SOK"),
        block("Mirrodin", "MRD", "DST", "5DN"),
        set("Eighth Edition", "8ED"),
        block("Onslaught", "ONS", "LGN", "SCG"),
        block("Odyssey", "ODY", "TOR", "JUD"),
        set("Seventh Edition", "7ED"),
        block("Invasion", "INV", "PLS", "APC"),
        block("Masques", "MMQ", "NEM", "PCY"),
        set("Classic Sixth Edition", "6ED"),
        block("Urza's", "USG", "ULG", "UDS"),
        block("Tempest", "TMP", "STH", "EXO"),
        set("Fifth Edition", "5ED"),
        block("Mirage", "MIR", "VIS", "WTH"),
        // Coldsnap (2006) closed out the Ice Age block when it was released.
        block("Ice Age", "ICE", "ALL", "CSP"),
        set("Homelands", "HML"),
        set("Chronicles", "CHR"),
        set("Fourth Edition", "4ED"),
        set("Fallen Empires", "FEM"),
        set("The Dark", "DRK"),
        set("Revised Edition", "3ED"),
        set("Legends", "LEG"),
        set("Antiquities", "ATQ"),
        set("Arabian Nights", "ARN"),
        set("Unlimited Edition", "2ED"),
        set("Limited Edition Beta", "LEB"),
        set("Limited Edition Alpha", "LEA"),
    )

    private val poolBySetCode: Map<String, Pool> =
        pools.flatMap { pool -> pool.setCodes.map { it to pool } }.toMap()

    /** The block or standalone set [setCode] belongs to, or null when the set isn't Primordial-legal. */
    fun poolOf(setCode: String): Pool? = poolBySetCode[setCode.uppercase()]

    /**
     * The banned list, by English name: the ante and dexterity cards, Shahrazad, the seven cards
     * Wizards removed for racist or culturally offensive content, and the format's own bans. The
     * rules page lists every ban without a set scope, so each applies in every pool.
     */
    val banned: Set<String> = setOf(
        // Ante
        "Amulet of Quoz", "Bronze Tablet", "Contract from Below", "Darkpact", "Demonic Attorney",
        "Jeweled Bird", "Rebirth", "Tempest Efreet", "Timmerian Fiends",
        // Dexterity
        "Chaos Orb", "Falling Star",
        "Shahrazad",
        // Racist or culturally offensive
        "Invoke Prejudice", "Cleanse", "Stone-Throwing Devils", "Pradesh Gypsies", "Jihad",
        "Imprison", "Crusade",
        // Primordial-specific
        "Channel", "Cranial Plating", "Merfolk Secretkeeper", "Sinkhole", "Skullclamp",
    )

    fun isBanned(cardName: String): Boolean = cardName in banned
}
