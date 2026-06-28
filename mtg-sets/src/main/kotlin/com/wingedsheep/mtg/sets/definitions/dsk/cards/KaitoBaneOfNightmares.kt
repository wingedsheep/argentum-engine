package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsu
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Kaito, Bane of Nightmares
 * {2}{U}{B}
 * Legendary Planeswalker — Kaito
 * Loyalty 4
 *
 * Ninjutsu {1}{U}{B} ({1}{U}{B}, Return an unblocked attacker you control to hand: Put this card
 * onto the battlefield from your hand tapped and attacking.)
 * During your turn, as long as Kaito has one or more loyalty counters on him, he's a 3/4 Ninja
 * creature and has hexproof.
 * +1: You get an emblem with "Ninjas you control get +1/+1."
 * 0: Surveil 2. Then draw a card for each opponent who lost life this turn.
 * −2: Tap target creature. Put two stun counters on it.
 *
 * Implementation notes:
 *  - Ninjutsu reuses the engine's shared declare-blockers alternative-cost pipeline (see the
 *    `ninjutsu(...)` helper / `KeywordAbility.ninjutsuStyleCost`). Because Kaito's own static makes
 *    him a creature on your turn, when he's put onto the battlefield via ninjutsu during your
 *    combat he enters tapped and attacking the defender the returned creature was attacking
 *    (CR 506.3a).
 *  - The animation is a *type-setting* continuous effect (CR ruling 2024-09-20): during your turn,
 *    while he has at least one loyalty counter, he stops being a planeswalker and is a 3/4 Ninja
 *    creature with hexproof. Modeled as conditional static abilities that add CREATURE + Ninja,
 *    remove PLANESWALKER, set base P/T 3/4, and grant hexproof — all gated on
 *    (it's your turn) AND (he has a loyalty counter). Removing the planeswalker type is what makes
 *    combat/noncombat damage mark on him as a creature instead of removing loyalty; his loyalty
 *    abilities remain activatable because that gate keys off the ability, not his current type.
 */
val KaitoBaneOfNightmares = card("Kaito, Bane of Nightmares") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Planeswalker — Kaito"
    startingLoyalty = 4
    oracleText = "Ninjutsu {1}{U}{B} ({1}{U}{B}, Return an unblocked attacker you control to hand: " +
        "Put this card onto the battlefield from your hand tapped and attacking.)\n" +
        "During your turn, as long as Kaito has one or more loyalty counters on him, he's a 3/4 " +
        "Ninja creature and has hexproof.\n" +
        "+1: You get an emblem with \"Ninjas you control get +1/+1.\"\n" +
        "0: Surveil 2. Then draw a card for each opponent who lost life this turn.\n" +
        "−2: Tap target creature. Put two stun counters on it."

    ninjutsu("{1}{U}{B}")

    // "During your turn, as long as Kaito has one or more loyalty counters on him, he's a 3/4
    // Ninja creature and has hexproof." A type-setting self-animation (he stops being a
    // planeswalker, per the official ruling) gated on your-turn AND has-a-loyalty-counter.
    val animationActive = Conditions.All(
        Conditions.IsYourTurn,
        Conditions.SourceHasCounter(CounterTypeFilter.Loyalty)
    )
    staticAbility {
        condition = animationActive
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }
    staticAbility {
        condition = animationActive
        ability = RemoveCardType("PLANESWALKER", GroupFilter.source())
    }
    staticAbility {
        condition = animationActive
        ability = GrantSubtype("Ninja", GroupFilter.source())
    }
    staticAbility {
        condition = animationActive
        ability = SetBasePowerToughnessStatic(3, 4, GroupFilter.source())
    }
    staticAbility {
        condition = animationActive
        ability = GrantKeyword(Keyword.HEXPROOF, GroupFilter.source())
    }

    // +1: You get an emblem with "Ninjas you control get +1/+1."
    loyaltyAbility(+1) {
        effect = Effects.CreatePermanentEmblem(
            groupFilter = GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.NINJA).youControl()
            ),
            powerBonus = 1,
            toughnessBonus = 1,
            emblemDescription = "Ninjas you control get +1/+1."
        )
    }

    // 0: Surveil 2. Then draw a card for each opponent who lost life this turn.
    loyaltyAbility(0) {
        effect = Patterns.Library.surveil(2) then
            Effects.DrawCards(DynamicAmounts.opponentsWhoLostLifeThisTurn())
    }

    // −2: Tap target creature. Put two stun counters on it.
    loyaltyAbility(-2) {
        val creature = target("creature", Targets.Creature)
        effect = Effects.Tap(creature) then Effects.AddCounters(Counters.STUN, 2, creature)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "220"
        artist = "Joshua Raphael"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55a14f30-4ff9-4472-90a6-c3139f1c18e5.jpg?1726286689"

        ruling("2024-09-20", "The ninjutsu ability can be activated only after blockers have been declared. Before then, attacking creatures are neither blocked nor unblocked.")
        ruling("2024-09-20", "Kaito enters attacking the same player, planeswalker, or battle the returned creature was attacking. This is a rule specific to ninjutsu.")
        ruling("2024-09-20", "Although Kaito enters attacking when his ninjutsu ability resolves, he was never declared as an attacking creature (for purposes of abilities that trigger whenever a creature attacks, for example).")
        ruling("2024-09-20", "Kaito's second ability causes him to stop being a planeswalker during your turn as long as he has at least one loyalty counter on him. While he's a creature, damage dealt to him won't cause loyalty counters to be removed. You can still activate Kaito's loyalty abilities.")
        ruling("2024-09-20", "Kaito's fourth ability cares whether opponents lost life this turn, not how their life totals changed. An opponent who gained 2 life and lost 1 life in the same turn still lost life that turn.")
    }
}
