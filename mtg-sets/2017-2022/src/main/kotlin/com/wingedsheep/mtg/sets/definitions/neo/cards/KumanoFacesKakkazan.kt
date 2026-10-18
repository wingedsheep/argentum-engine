package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kumano Faces Kakkazan // Etching of Kumano
 * {R} — Enchantment — Saga // Enchantment Creature — Human Shaman 2/2
 *
 * Front — Kumano Faces Kakkazan:
 *   I — This Saga deals 1 damage to each opponent and each planeswalker they control.
 *   II — When you next cast a creature spell this turn, that creature enters with an additional
 *        +1/+1 counter on it.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Etching of Kumano:
 *   Haste
 *   If a creature dealt damage this turn by a source you controlled would die, exile it instead.
 *
 * Chapter II is Long List of the Ents' one-shot delayed cast trigger: the counter goes on the spell
 * on the stack and travels with it onto the battlefield. Etching's replacement reads each dying
 * creature's own record of who controlled the sources that damaged it this turn
 * (`wasDealtDamageThisTurnBy(Any.youControl())`), so the source needn't still be on the battlefield.
 */
private val EtchingOfKumano = card("Etching of Kumano") {
    manaCost = ""
    colorIndicator = "R"
    colorIdentity = "R"
    typeLine = "Enchantment Creature — Human Shaman"
    oracleText = "Haste\n" +
        "If a creature dealt damage this turn by a source you controlled would die, exile it instead."
    power = 2
    toughness = 2

    keywords(Keyword.HASTE)

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.wasDealtDamageThisTurnBy(GameObjectFilter.Any.youControl()),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD,
            ),
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "152"
        artist = "Mike Bierek"
        flavorText = "Many kami came to demand a secret he did not know, and every one of them fell."
        imageUri = "https://cards.scryfall.io/normal/back/2/8/28d92cad-45b0-479d-97fc-c175f3a3f259.jpg?1783923870"
    }
}

private val KumanoFacesKakkazanFront = card("Kumano Faces Kakkazan") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I — This Saga deals 1 damage to each opponent and each planeswalker they control.\n" +
        "II — When you next cast a creature spell this turn, that creature enters with an additional " +
        "+1/+1 counter on it.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    sagaChapter(1) {
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Patterns.Group.dealDamageToAll(1, GroupFilter(GameObjectFilter.Planeswalker.opponentControls()))
    }

    sagaChapter(2) {
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.you.casts(GameObjectFilter.Creature),
            fireOnce = true,
            expiry = DelayedTriggerExpiry.EndOfTurn,
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity)
        )
    }

    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "152"
        artist = "Mike Bierek"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28d92cad-45b0-479d-97fc-c175f3a3f259.jpg?1783923870"
    }
}

val KumanoFacesKakkazan: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = KumanoFacesKakkazanFront,
    backFace = EtchingOfKumano,
)
