package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ballista Watcher // Ballista Wielder (Innistrad: Crimson Vow)
 * {2}{R}{R}
 * Creature — Human Soldier Werewolf // Creature — Werewolf
 *
 * Front — Ballista Watcher (4/3): "{2}{R}, {T}: This creature deals 1 damage to any target"; Daybound.
 * Back  — Ballista Wielder (5/5): "{2}{R}: This creature deals 1 damage to any target. A creature dealt
 *          damage this way can't block this turn"; Nightbound.
 *
 * A pinger that loses its `{T}` and gains a can't-block rider at night. The front's ability taps (a
 * [Costs.Composite] of mana and [Costs.Tap]); the back's costs only mana. Both deal 1 to a single
 * [Targets.Any] (any target), with `damageSource = EffectTarget.Self` so the damage is dealt *by this
 * creature* (matters for Howlpack Avenger / lifelink-style riders elsewhere).
 *
 * The back's "a creature dealt damage this way can't block this turn" is an [Effects.CantBlock] over
 * the *same* targeted object as the damage ([EffectTarget.ContextTarget] 0). Because the target is "any
 * target", it may be a player or planeswalker; [Effects.CantBlock]'s executor no-ops on a non-creature
 * (it requires a `CardComponent` and only creatures can block anyway), so the rider bites exactly and
 * only when the damaged object is a creature — matching the oracle wording without a separate
 * creature-only target.
 *
 * The back is a transformed face with no mana cost, so its color comes from a color indicator (CR 204):
 * `colorIndicator = "R"`.
 */

private val BallistaWatcherFront = card("Ballista Watcher") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Soldier Werewolf"
    power = 4
    toughness = 3
    oracleText = "{2}{R}, {T}: This creature deals 1 damage to any target.\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.Tap)
        val anyTarget = target("any target", Targets.Any)
        effect = Effects.DealDamage(1, anyTarget, damageSource = EffectTarget.Self)
        description = "This creature deals 1 damage to any target."
    }
    daybound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Tomas Duchek"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63d96c52-66ce-4b46-9a0b-7cd9a43f9253.jpg?1783924852"
    }
}

private val BallistaWielder = card("Ballista Wielder") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 5
    toughness = 5
    oracleText = "{2}{R}: This creature deals 1 damage to any target. A creature dealt damage this way " +
        "can't block this turn.\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    activatedAbility {
        cost = Costs.Mana("{2}{R}")
        val anyTarget = target("any target", Targets.Any)
        effect = Effects.Composite(
            Effects.DealDamage(1, anyTarget, damageSource = EffectTarget.Self),
            Effects.CantBlock(anyTarget),
        )
        description = "This creature deals 1 damage to any target. A creature dealt damage this way " +
            "can't block this turn."
    }
    nightbound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Tomas Duchek"
        imageUri = "https://cards.scryfall.io/normal/back/6/3/63d96c52-66ce-4b46-9a0b-7cd9a43f9253.jpg?1783924852"
    }
}

val BallistaWatcher: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = BallistaWatcherFront,
    backFace = BallistaWielder,
)
