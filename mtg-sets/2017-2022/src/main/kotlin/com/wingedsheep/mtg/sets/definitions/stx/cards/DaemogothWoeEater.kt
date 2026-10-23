package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Daemogoth Woe-Eater — Strixhaven: School of Mages #175 (canonical printing)
 * {1}{B}{B/G}{G} · Creature — Demon · 7/6
 *
 * At the beginning of your upkeep, sacrifice a creature.
 * When you sacrifice this creature, each opponent discards a card, you draw a card, and you gain
 * 2 life.
 *
 * The upkeep tax is the Devouring Strossus shape ([Effects.SacrificeOwn]; the Woe-Eater itself is
 * a legal choice). "When you sacrifice this creature" is [Triggers.self.isSacrificed] — a sacrifice
 * only, never a destroy — so it fires whether the upkeep trigger or any other effect or cost
 * sacrificed it (ruling).
 */
val DaemogothWoeEater = card("Daemogoth Woe-Eater") {
    manaCost = "{1}{B}{B/G}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Demon"
    oracleText = "At the beginning of your upkeep, sacrifice a creature.\n" +
        "When you sacrifice this creature, each opponent discards a card, you draw a card, and you " +
        "gain 2 life."
    power = 7
    toughness = 6

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.SacrificeOwn(GameObjectFilter.Creature)
    }

    triggeredAbility {
        trigger = Triggers.self.isSacrificed()
        effect = Effects.Discard(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.DrawCards(1) then
            Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "175"
        artist = "Alex Brock"
        flavorText = "\"Your pain for my wisdom. You'll get no better deal.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c965cbe3-50e5-41bb-bdb9-e65d97aab716.jpg?1783927318"
        ruling("2021-04-16", "Daemogoth Woe-Eater's second ability will trigger if you sacrifice it as its first ability resolves. It will also trigger if you sacrifice it for any other reason.")
    }
}
