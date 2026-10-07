package com.wingedsheep.mtg.sets.definitions.fra

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.EchoedPairsPlayBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Reality Fracture (2026)
 *
 * Scaffolded to hold the canonical [CardDefinition]s of cards whose earliest real printing is
 * Reality Fracture, with later sets contributing reprint [Printing] rows.
 *
 * Set Code: FRA
 * Release Date: 2026-10-02
 */
object RealityFractureSet : MtgSet {

    override val code = "FRA"
    override val displayName = "Reality Fracture"
    override val releaseDate = "2026-10-02"

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    /** Every Play Booster opens one complete echoed pair plus an echoed card from another pair. */
    override val boosterStrategy: BoosterStrategy by lazy { EchoedPairsPlayBooster(echoedPairs = ECHOED_PAIRS) }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.fra.cards"

    /** The 43 echoed pairs (collector numbers 195–280), from the set's collecting article. */
    private val ECHOED_PAIRS: List<Pair<String, String>> = listOf(
        // Mythic
        "Ajani Resolute" to "Ajani Unrelenting",
        "Chandra, Chill of Compliance" to "Chandra, Torch of Defiance",
        "Garruk, Veiled Butcher" to "Garruk, Curse Breaker",
        // Rare
        "Gideon's Memorial" to "Gideon the Oathless",
        "Liliana the Faultless" to "Liliana the Repentant",
        "Lyra, Archangel of Dawn" to "Lyra, Tolarian Archangel",
        "Jace, Reality Sculptor" to "Tam, the Possibility",
        "Samut, Tyrant of Naktamun" to "Samut, Hazoret's Champion",
        "Karn, Gilded Guardian" to "Karn, Argent Defender",
        "Vraska, Soul of Stone" to "Vraska, the Cutting Glare",
        // Uncommon
        "Danitha, Sword of Hope" to "Danitha, Spear of Agony",
        "Ghalta the Immovable" to "Ghalta the Unstoppable",
        "Koth of the Homestead" to "Koth, the Geomancer",
        "Rescue Girl, First Responder" to "Massacre Girl, Most Wanted",
        "Saheeli, Consul of Oversight" to "Saheeli, Jewel of Avishkar",
        "Teyo, Lightshield Expert" to "Teyo, Diamondblade Mage",
        "Thalia, the Survivor" to "Geist of Saint Thalia",
        "Tomik, Orzhov Lawmage" to "Tomik, Izzet Sparkmage",
        "Way of the Healer" to "Way of the Necromancer",
        "Way of the Mentor" to "Way of the Warlord",
        "Yoshimaru, Beloved Companion" to "Yoshimaru, Scrappy Stray",
        "Yuriko, Blade of the Mighty" to "Yuriko, Hope from the Shadows",
        "Arni, Humble Scribe" to "Arni, Renowned Champion",
        "Fblthp, Impossibly Lost" to "Fblthp, Knows the Way",
        "Hapatra, the Desert Frost" to "Hapatra, the Desert Fang",
        "Proft, Consulting Detective" to "Proft, Sinister Mastermind",
        "Ruric Thar, Biomagus" to "Ruric Thar, Magecrusher",
        "Tetsuko Umezawa, Fugitive" to "Tetsuko Umezawa, Pursuer",
        "Traxos, Academy Guardian" to "Traxos, Scourge Eternal",
        "Way of the Cryomancer" to "Way of the Pyromancer",
        "Way of the Mind Sculptor" to "Way of the Paradox",
        "Yargle, Goliath of Otaria" to "Yargle, Glutton of Urborg",
        "Gallia, Tragic Host" to "Gallia, the Merrymaker",
        "Loot, the Anomaly" to "Loot, the Nexus",
        "Mabel, Bitter Recluse" to "Mabel, Valley Hero",
        "Tinybones, Pocket Nuisance" to "Titanbones, Towering Heart",
        "Way of the Deathbringer" to "Way of the Wildspeaker",
        "Winter, Tormented Loner" to "Winter, Team Player",
        "Jiang Yanggu, Alone" to "Jiang Yanggu, Never Alone",
        "Kiora of Fire and Ashes" to "Kiora of Salt and Sand",
        "Marwyn, the Clearcutter" to "Marwyn, the Preserver",
        "Pia, Determined Rebuilder" to "Pia, Aether Ascetic",
        "Edgar, Moonlit Sovereign" to "Edgar, Ancient Bloodlord",
    )
}
