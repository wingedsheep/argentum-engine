package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Black Widow, Double Agent — Marvel Super Heroes #208
 * {1}{W}{B} · Legendary Creature — Human Hero Villain · 3/2
 *
 * Deathtouch
 * Whenever a creature you control attacks alone, it gains first strike and menace until end of
 * turn.
 *
 * Not to be confused with [BlackWidowSuperSpy] (MSH #89) — a different card that shares only the
 * character.
 *
 * Modeling notes:
 *  - The set's established "attacks alone" shape (Agent 13, Agents of S.H.I.E.L.D., Hydra
 *    Infiltration): an ANY-bound [Triggers.attacks] filtered to creatures you control with
 *    [AttackPredicate.Alone]. Black Widow herself qualifies when she is the lone attacker, and
 *    so does any other creature you control attacking on its own — she does not have to attack.
 *  - "It" is the lone attacker ([EffectTarget.TriggeringEntity]), not Black Widow, so both
 *    grants land on the triggering creature. Nothing here targets: the ability picks its
 *    subject from the trigger, which means an attacker with hexproof or protection still gets
 *    the keywords.
 */
val BlackWidowDoubleAgent = card("Black Widow, Double Agent") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Human Hero Villain"
    power = 3
    toughness = 2
    oracleText = "Deathtouch\n" +
        "Whenever a creature you control attacks alone, it gains first strike and menace until " +
        "end of turn. (It can't be blocked except by two or more creatures.)"

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.attacks(
            filter = GameObjectFilter.Creature.youControl(),
            requires = setOf(AttackPredicate.Alone),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.TriggeringEntity) then
            Effects.GrantKeyword(Keyword.MENACE, EffectTarget.TriggeringEntity)
        description = "Whenever a creature you control attacks alone, it gains first strike and " +
            "menace until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "208"
        artist = "Michael MacRae"
        flavorText = "\"Careful, I bite.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa8dbbb9-36a5-48e0-9e30-aeed0fb5d522.jpg?1783902905"
    }
}
