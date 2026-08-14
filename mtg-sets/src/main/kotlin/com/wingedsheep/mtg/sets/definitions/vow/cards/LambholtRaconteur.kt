package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.daybound
import com.wingedsheep.sdk.dsl.nightbound
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lambholt Raconteur // Lambholt Ravager (Innistrad: Crimson Vow)
 * {3}{R}
 * Creature — Human Warrior Werewolf // Creature — Werewolf
 *
 * Front — Lambholt Raconteur (2/4): "Whenever you cast a noncreature spell, this creature deals 1 damage
 *          to each opponent"; Daybound.
 * Back  — Lambholt Ravager (4/4): "Whenever you cast a noncreature spell, this creature deals 2 damage to
 *          each opponent"; Nightbound.
 *
 * A noncreature-cast pinger, following Thermo-Alchemist's [Triggers.YouCastNoncreature] +
 * [DealDamageEffect] to [EffectTarget.PlayerRef]([Player.EachOpponent]) rail. The night face doubles the
 * damage to 2. The back is a transformed face with no mana cost, so its color comes from a color
 * indicator (CR 204): `colorIndicator = "R"`.
 */

private val LambholtRaconteurFront = card("Lambholt Raconteur") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior Werewolf"
    power = 2
    toughness = 4
    oracleText = "Whenever you cast a noncreature spell, this creature deals 1 damage to each opponent.\n" +
        "Daybound (If a player casts no spells during their own turn, it becomes night next turn.)"

    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        effect = DealDamageEffect(1, EffectTarget.PlayerRef(Player.EachOpponent), damageSource = EffectTarget.Self)
        description = "This creature deals 1 damage to each opponent."
    }
    daybound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "167"
        artist = "Andrey Kuzinskiy"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0cbee24c-9147-46cb-a5f9-8d919c021aa4.jpg?1783924839"
    }
}

private val LambholtRavager = card("Lambholt Ravager") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Werewolf"
    power = 4
    toughness = 4
    oracleText = "Whenever you cast a noncreature spell, this creature deals 2 damage to each opponent.\n" +
        "Nightbound (If a player casts at least two spells during their own turn, it becomes day next turn.)"

    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        effect = DealDamageEffect(2, EffectTarget.PlayerRef(Player.EachOpponent), damageSource = EffectTarget.Self)
        description = "This creature deals 2 damage to each opponent."
    }
    nightbound()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "167"
        artist = "Andrey Kuzinskiy"
        imageUri = "https://cards.scryfall.io/normal/back/0/c/0cbee24c-9147-46cb-a5f9-8d919c021aa4.jpg?1783924839"
    }
}

val LambholtRaconteur: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = LambholtRaconteurFront,
    backFace = LambholtRavager,
)
