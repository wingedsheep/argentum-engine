package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Zero, Incorporeal Sensate
 * {1}{W/B}{W/B}
 * Legendary Artifact Creature — Medusan Scientist
 * 2/2
 *
 * Lifelink
 * When Zero enters, look at target opponent's hand. You may exile a nonland card from it. For as
 * long as that card remains exiled, its owner may play it. A spell cast this way costs {2} more to
 * cast.
 *
 * Elite Spellbinder's ETB verbatim: a hand look, an optional nonland pick, then
 * [Effects.ExileAndGrantOwnerPlayPermission] for the owner's lasting play permission and the {2} tax
 * (the card came from an opponent's hand, so its owner is always an opponent of Zero's controller).
 */
val ZeroIncorporealSensate = card("Zero, Incorporeal Sensate") {
    manaCost = "{1}{W/B}{W/B}"
    colorIdentity = "WB"
    typeLine = "Legendary Artifact Creature — Medusan Scientist"
    power = 2
    toughness = 2
    oracleText = "Lifelink\n" +
        "When Zero enters, look at target opponent's hand. You may exile a nonland card from it. " +
        "For as long as that card remains exiled, its owner may play it. A spell cast this way " +
        "costs {2} more to cast."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.LookAtHand(opponent) then
            Effects.Pipeline {
                val opponentHand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
                val exiled = chooseUpTo(
                    1,
                    from = opponentHand,
                    chooser = Chooser.Controller,
                    filter = GameObjectFilter.Nonland,
                    prompt = "You may exile a nonland card from target opponent's hand",
                    showAllCards = true,
                    alwaysPrompt = true
                )
                ifNotEmpty(exiled) {
                    run(Effects.ExileAndGrantOwnerPlayPermission(
                        target = exiled.asTarget,
                        opponentCostIncrease = 2
                    ))
                }
            }
        description = "When Zero enters, look at target opponent's hand. You may exile a nonland " +
            "card from it. For as long as that card remains exiled, its owner may play it. A spell " +
            "cast this way costs {2} more to cast."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "258"
        artist = "Racrufi"
        flavorText = "\"This will either be spectacular or a terrible end to our adventure.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93f616fe-6906-4e5d-b9fe-b432cccdb0ec.jpg?1792508418"
    }
}
