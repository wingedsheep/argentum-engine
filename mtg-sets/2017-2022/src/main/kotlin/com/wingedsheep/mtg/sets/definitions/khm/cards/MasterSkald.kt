package com.wingedsheep.mtg.sets.definitions.khm.cards

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
 * Master Skald — Kaldheim #19
 * {4}{W} · Creature — Dwarf Warrior · 4/4
 *
 * When this creature enters, you may exile a creature card from your graveyard. If you do,
 * return target artifact or enchantment card from your graveyard to your hand.
 *
 * The target is chosen as the trigger goes on the stack; the exile is chosen on resolution
 * (the "exile one" pipeline gated by [Effects.IfYouDo], as on Skywarp Skaab). The return reads
 * `fromZone = GRAVEYARD`, so if the player exiles the targeted artifact/enchantment creature card
 * itself, nothing returns — matching the 2021-02-05 ruling.
 */
val MasterSkald = card("Master Skald") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Warrior"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, you may exile a creature card from your graveyard. " +
        "If you do, return target artifact or enchantment card from your graveyard to your hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val returned = target(
            TargetFilter(GameObjectFilter.ArtifactOrEnchantment.ownedByYou(), zone = Zone.GRAVEYARD),
        )
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
                then = Effects.ReturnToHandFromGraveyard(returned),
                successCriterion = SuccessCriterion.CollectionNonEmpty("toExile", min = 1)
            )
        )
        description = "When this creature enters, you may exile a creature card from your graveyard. " +
            "If you do, return target artifact or enchantment card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Svetlin Velinov"
        flavorText = "\"All my scars have stories to tell.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b269e27-8e17-4bf2-b28a-a83ecc7d22c0.jpg?1783928282"
        ruling(
            "2021-02-05",
            "You choose the target artifact or enchantment card in your graveyard as you put the " +
                "triggered ability on the stack. Once the ability starts resolving, you choose which " +
                "creature card to exile from your graveyard, if any."
        )
        ruling(
            "2021-02-05",
            "If you choose an artifact creature card or an enchantment creature card in your graveyard " +
                "as the target, you could exile that card as the triggered ability is resolving, but if " +
                "you do, you won't return it to your hand."
        )
    }
}
