package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Cataclysmic Gearhulk
 * {3}{W}{W}
 * Artifact Creature — Construct
 * 4/5
 * Vigilance
 * When this creature enters, each player chooses an artifact, a creature, an enchantment, and a
 * planeswalker from among the nonland permanents they control, then sacrifices the rest.
 *
 * The Liliana, Dreadhorde General −9 shape over every player's nonland permanents:
 * `chooseOnePerCategory` asks each controller in APNAP order (the active player first, per the
 * ruling) for one artifact, creature, enchantment and planeswalker to keep — one permanent may
 * answer several categories, and an empty category is skipped — then every unchosen nonland
 * permanent is sacrificed at once. The Gearhulk is a candidate like any other permanent; lands,
 * artifact lands included, are never candidates and never sacrificed.
 */
val CataclysmicGearhulk = card("Cataclysmic Gearhulk") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Construct"
    power = 4
    toughness = 5
    oracleText = "Vigilance\nWhen this creature enters, each player chooses an artifact, a creature, an " +
        "enchantment, and a planeswalker from among the nonland permanents they control, then sacrifices " +
        "the rest."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val atRisk = gather(Filters.NonlandPermanent)
            val kept = chooseOnePerCategory(
                atRisk,
                listOf(
                    GameObjectFilter.Artifact,
                    GameObjectFilter.Creature,
                    GameObjectFilter.Enchantment,
                    GameObjectFilter.Planeswalker,
                ),
            )
            sacrifice(exclude(atRisk, kept))
        }
        description = "When this creature enters, each player chooses an artifact, a creature, an " +
            "enchantment, and a planeswalker from among the nonland permanents they control, then " +
            "sacrifices the rest."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "9"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b10947b-c276-41c4-b72c-efcddd94d13b.jpg?1783937235"
        ruling(
            "2016-09-20",
            "First the player whose turn it is chooses permanents they control, then each other player in turn " +
                "order does the same. Each player will know the choices made by players who chose before them. " +
                "All of the unchosen nonland permanents are then sacrificed simultaneously."
        )
        ruling(
            "2016-09-20",
            "A permanent with more than one type may be chosen as either of the types or both. For example, an " +
                "artifact creature may be chosen as the artifact, the creature, or both. Choosing the same " +
                "permanent twice this way has the same result as choosing it once."
        )
        ruling(
            "2016-09-20",
            "Cataclysmic Gearhulk will be sacrificed to its own ability if you don't choose it as your artifact " +
                "or your creature."
        )
        ruling(
            "2016-09-20",
            "Lands can't be chosen and won't be sacrificed, even if they have other types (such as an artifact land)."
        )
        ruling(
            "2016-09-20",
            "If you don't control any permanents of one of the types, you'll still choose the permanents of the " +
                "types you do control."
        )
    }
}
