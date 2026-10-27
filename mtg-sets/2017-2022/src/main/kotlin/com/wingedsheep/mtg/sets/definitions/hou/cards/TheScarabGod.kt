package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Scarab God — Hour of Devastation #145 (canonical printing)
 * {3}{U}{B} · Legendary Creature — God · 5/5
 *
 * At the beginning of your upkeep, each opponent loses X life and you scry X, where X is the
 * number of Zombies you control.
 * {2}{U}{B}: Exile target creature card from a graveyard. Create a token that's a copy of it,
 * except it's a 4/4 black Zombie.
 * When The Scarab God dies, return it to its owner's hand at the beginning of the next end step.
 *
 * X counts Zombie permanents you control as the upkeep trigger resolves (ruling), once for the
 * life loss and once for the scry.
 *
 * The reanimation is Nahiri, the Unforgiving's exile-then-copy shape: the target handle still names
 * the card after it moves to exile, and the token copies its printed characteristics. "A 4/4 black
 * Zombie" states a color and a creature type without "in addition to", so both *replace* the copied
 * ones (CR 205.1a) — `overrideColors` / `overrideSubtypes` — while card types and supertypes are
 * kept (an artifact creature card stays an artifact creature).
 *
 * The dies trigger schedules a one-shot at the next end step (any player's — "the next end step",
 * not "your") that moves the card from the graveyard to its owner's hand; if it has already left
 * the graveyard by then it stays where it is (ruling). The card is named through the dies
 * trigger's triggering entity (Liesa, Forgotten Archangel), which is baked with its graveyard identity.
 */
val TheScarabGod = card("The Scarab God") {
    manaCost = "{3}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — God"
    power = 5
    toughness = 5
    oracleText = "At the beginning of your upkeep, each opponent loses X life and you scry X, where X is the number of Zombies you control.\n" +
        "{2}{U}{B}: Exile target creature card from a graveyard. Create a token that's a copy of it, except it's a 4/4 black Zombie.\n" +
        "When The Scarab God dies, return it to its owner's hand at the beginning of the next end step."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        val zombies = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Permanent.withSubtype("Zombie")).count()
        effect = Effects.LoseLife(zombies, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.Scry(zombies)
    }

    activatedAbility {
        cost = Costs.Mana("{2}{U}{B}")
        val creatureCard = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.Exile(creatureCard, fromZone = Zone.GRAVEYARD) then
            Effects.CreateTokenCopyOfTarget(
                target = creatureCard,
                overridePower = 4,
                overrideToughness = 4,
                overrideColors = setOf(Color.BLACK),
                overrideSubtypes = setOf(Subtype("Zombie")),
            )
        description = "{2}{U}{B}: Exile target creature card from a graveyard. Create a token that's a copy of it, " +
            "except it's a 4/4 black Zombie."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateDelayedTrigger(
            step = Step.END,
            effect = Effects.Move(EffectTarget.TriggeringEntity, Zone.HAND, fromZone = Zone.GRAVEYARD),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "145"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/621cfd79-be4e-41e3-95ce-62b0eeff5baf.jpg?1783936010"
        ruling("2020-08-07", "The number of Zombies you control is counted as The Scarab God's first ability resolves. Players can try to change that number in response to the ability (perhaps by activating its second ability).")
        ruling("2017-07-14", "If this creature dies but leaves your graveyard before the next end step, it will remain in its new zone.")
        ruling("2017-07-14", "The \"next end step\" refers to the next end step that occurs, not the end step of the next turn. If this creature dies before a turn's end step (for example, during combat), it will be returned to its owner's hand at the beginning of that turn's end step.")
    }
}
