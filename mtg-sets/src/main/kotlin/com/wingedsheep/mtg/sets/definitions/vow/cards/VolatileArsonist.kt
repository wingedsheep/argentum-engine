package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Volatile Arsonist // Dire-Strain Anarchist (Innistrad: Crimson Vow)
 * {3}{R}{R}
 * Creature — Human Werewolf // Creature — Werewolf
 *
 * Front — Volatile Arsonist (4/4): Menace, haste; "Whenever this creature attacks, it deals 1 damage to
 *          each of up to one target creature, up to one target player, and/or up to one target
 *          planeswalker"; Daybound.
 * Back  — Dire-Strain Anarchist (5/5): Menace, haste; same attack trigger dealing 2 damage; Nightbound.
 *
 * The attack trigger has **three independent "up to one target" slots** — a creature, a player, and a
 * planeswalker — each chosen separately at trigger time (CR 601.2c / "and/or"). Modeled as three optional
 * targets plus a [Effects.Composite] of three [Effects.DealDamage] instances, one per slot, all sourced
 * from this creature (`damageSource = EffectTarget.Self`). [Effects.DealDamage] no-ops on an unchosen
 * optional slot (same rail as Conduct Electricity's "up to one target creature token"), so declining any
 * subset — including all three — is legal and simply deals no damage there.
 *
 * The night face raises each instance to 2. The back is a transformed face with no mana cost, so its
 * color comes from a color indicator (CR 204): `colorIndicator = "R"`.
 */

private val VolatileArsonistFront = card("Volatile Arsonist") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Werewolf"
    power = 4
    toughness = 4
    oracleText = "Menace, haste\n" +
        "Whenever this creature attacks, it deals 1 damage to each of up to one target creature, up to " +
        "one target player, and/or up to one target planeswalker.\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    keywords(Keyword.MENACE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.Attacks
        val creature = target("up to one target creature", TargetCreature(optional = true))
        val player = target("up to one target player", TargetPlayer(optional = true))
        val planeswalker = target(
            "up to one target planeswalker",
            TargetPermanent(optional = true, filter = TargetFilter.Planeswalker),
        )
        effect = Effects.Composite(
            Effects.DealDamage(1, creature, damageSource = EffectTarget.Self),
            Effects.DealDamage(1, player, damageSource = EffectTarget.Self),
            Effects.DealDamage(1, planeswalker, damageSource = EffectTarget.Self),
        )
        description = "This creature deals 1 damage to each of up to one target creature, up to one " +
            "target player, and/or up to one target planeswalker."
    }
    daybound()

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "181"
        artist = "Gabor Szikszai"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87a02ac1-c43a-43cc-9c2b-628cfdeb4cbf.jpg?1783924830"
    }
}

private val DireStrainAnarchist = card("Dire-Strain Anarchist") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 5
    toughness = 5
    oracleText = "Menace, haste\n" +
        "Whenever this creature attacks, it deals 2 damage to each of up to one target creature, up to " +
        "one target player, and/or up to one target planeswalker.\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    keywords(Keyword.MENACE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.Attacks
        val creature = target("up to one target creature", TargetCreature(optional = true))
        val player = target("up to one target player", TargetPlayer(optional = true))
        val planeswalker = target(
            "up to one target planeswalker",
            TargetPermanent(optional = true, filter = TargetFilter.Planeswalker),
        )
        effect = Effects.Composite(
            Effects.DealDamage(2, creature, damageSource = EffectTarget.Self),
            Effects.DealDamage(2, player, damageSource = EffectTarget.Self),
            Effects.DealDamage(2, planeswalker, damageSource = EffectTarget.Self),
        )
        description = "This creature deals 2 damage to each of up to one target creature, up to one " +
            "target player, and/or up to one target planeswalker."
    }
    nightbound()

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "181"
        artist = "Gabor Szikszai"
        imageUri = "https://cards.scryfall.io/normal/back/8/7/87a02ac1-c43a-43cc-9c2b-628cfdeb4cbf.jpg?1783924830"
    }
}

val VolatileArsonist: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = VolatileArsonistFront,
    backFace = DireStrainAnarchist,
)
