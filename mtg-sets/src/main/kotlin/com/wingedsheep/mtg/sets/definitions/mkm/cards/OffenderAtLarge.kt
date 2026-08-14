package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Offender at Large — Murders at Karlov Manor #138
 * {4}{R} · Creature — Giant Rogue · 5/4
 *
 * Disguise {4}{R}
 * When this creature enters or is turned face up, up to one target creature gets +2/+0 until end
 * of turn.
 *
 * One ability with two trigger conditions, so it is a single `Triggers.or` of the two SELF-bound
 * event patterns rather than two abilities — it must fire exactly once on either route, never
 * twice. The routes are disjoint (CR 702.168d: turning face up is not entering the battlefield), so
 * a hard-cast Offender gets the pump on entry and a disguised one gets it when it flips, and
 * neither gets it twice.
 *
 * "**Up to one** target creature" is `optional = true`, not a mandatory target: the ability still
 * triggers and still resolves with no target chosen, which is the only sane behaviour when the
 * Offender flips face up at instant speed on an empty board. Modelling it as a required target
 * would remove the ability from the stack in exactly the case the "up to" wording exists to cover.
 *
 * The target is any creature, not "creature you control" — flipping it up mid-combat to hand +2/+0
 * to an opposing blocker is a legal, if unusual, line.
 */
val OffenderAtLarge = card("Offender at Large") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant Rogue"
    power = 5
    toughness = 4
    oracleText = "Disguise {4}{R} (You may cast this card face down for {3} as a 2/2 creature " +
        "with ward {2}. Turn it face up any time for its disguise cost.)\n" +
        "When this creature enters or is turned face up, up to one target creature gets +2/+0 " +
        "until end of turn."

    disguise = "{4}{R}"

    triggeredAbility {
        trigger = Triggers.or(Triggers.EntersBattlefield, Triggers.TurnedFaceUp)
        val creature = target("up to one target creature", TargetCreature(optional = true))
        effect = Effects.ModifyStats(2, 0, creature)
        description = "When this creature enters or is turned face up, up to one target creature " +
            "gets +2/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Mike Bierek"
        flavorText = "\"In a city this big, anyone can blend in.\"\n—Imel of the Foundway Associates"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f096ff4a-85f4-46f1-9478-e8921f21309d.jpg?1783912876"
    }
}
