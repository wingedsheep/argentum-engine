package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Cataclysmic Gearhulk
 * {3}{W}{W}
 * Artifact Creature — Construct
 * 4/5
 * Vigilance
 * When this creature enters, each player chooses an artifact, a creature, an enchantment, and a
 * planeswalker from among the nonland permanents they control, then sacrifices the rest.
 *
 * The Mythos of Snapdax shape: inside `forEachPlayerCollecting` each player makes four separate
 * one-of choices over their own nonland permanents (so an artifact creature can be kept as both the
 * artifact and the creature, and an empty category is simply skipped); once every player has
 * chosen, all unchosen nonland permanents are sacrificed at the same time. Per the ruling the
 * player whose turn it is chooses first ([Player.ActivePlayerFirst]), which matters when the
 * Gearhulk enters on another player's turn. The Gearhulk itself is a candidate like any other
 * permanent, so it's sacrificed unless its controller keeps it as their artifact or creature.
 * Lands, artifact lands included, are never candidates and never sacrificed.
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
            val categories = listOf(
                GameObjectFilter.Artifact to "an artifact",
                GameObjectFilter.Creature to "a creature",
                GameObjectFilter.Enchantment to "an enchantment",
                GameObjectFilter.Planeswalker to "a planeswalker",
            )
            val collected = forEachPlayerCollecting(Player.ActivePlayerFirst) {
                val nonland = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.NonlandPermanent))
                val kept = categories.map { (category, label) ->
                    chooseExactly(
                        1,
                        from = filter(nonland, category),
                        prompt = "Choose $label to keep. Unchosen nonland permanents are sacrificed.",
                        selectedLabel = "Keep",
                        remainderLabel = "Sacrifice",
                        useTargetingUI = true,
                    )
                }
                listOf(nonland) + kept
            }
            val atRisk = collected.first()
            val doomed = collected.drop(1).fold(atRisk) { remaining, kept -> exclude(remaining, kept) }
            sacrifice(doomed)
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
