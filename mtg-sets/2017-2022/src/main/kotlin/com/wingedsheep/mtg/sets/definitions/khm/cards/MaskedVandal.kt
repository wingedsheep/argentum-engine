package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Masked Vandal — Kaldheim #184
 * {1}{G} · Creature — Shapeshifter · 1/3
 *
 * Changeling
 * When this creature enters, you may exile a creature card from your graveyard. If you do,
 * exile target artifact or enchantment an opponent controls.
 *
 * The target is chosen as the trigger goes on the stack; the graveyard exile is chosen on
 * resolution — the same "exile one" pipeline gated by [Effects.IfYouDo] as Master Skald.
 */
val MaskedVandal = card("Masked Vandal") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Shapeshifter"
    power = 1
    toughness = 3
    oracleText = "Changeling (This card is every creature type.)\n" +
        "When this creature enters, you may exile a creature card from your graveyard. " +
        "If you do, exile target artifact or enchantment an opponent controls."

    keywords(Keyword.CHANGELING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.ArtifactOrEnchantment.opponentControls())
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.Pipeline {
                    val graveyardCreatures = gather(
                        CardSource.FromZone(zone = Zone.GRAVEYARD, filter = GameObjectFilter.Creature)
                    )
                    // Named: the "if you do" check below reads it from outside the pipeline.
                    val toExile = chooseExactly(1, from = graveyardCreatures, selectedLabel = "Exile", name = "toExile")
                    exile(toExile)
                },
                then = Effects.Exile(permanent),
                successCriterion = SuccessCriterion.CollectionNonEmpty("toExile", min = 1)
            )
        )
        description = "When this creature enters, you may exile a creature card from your graveyard. " +
            "If you do, exile target artifact or enchantment an opponent controls."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "184"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0a9c72a-e450-41e3-80e5-06f2f1171245.jpg?1783928209"
        ruling(
            "2021-02-05",
            "You choose the target artifact or enchantment an opponent controls as you put the " +
                "triggered ability on the stack. Once the ability starts resolving, you choose which " +
                "creature card to exile from your graveyard, if any. At that point, no one can respond " +
                "to your choice or take any actions until the ability is finished resolving."
        )
    }
}
