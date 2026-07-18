package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Preacher of the Schism
 * {2}{B}
 * Creature — Vampire Cleric
 * 2/4
 * Deathtouch
 * Whenever this creature attacks the player with the most life or tied for most life, create a 1/1
 * white Vampire creature token with lifelink.
 * Whenever this creature attacks while you have the most life or are tied for most life, you draw a
 * card and you lose 1 life.
 */
val PreacherOfTheSchism = card("Preacher of the Schism") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Cleric"
    oracleText = "Deathtouch\n" +
        "Whenever this creature attacks the player with the most life or tied for most life, create a " +
        "1/1 white Vampire creature token with lifelink.\n" +
        "Whenever this creature attacks while you have the most life or are tied for most life, you " +
        "draw a card and you lose 1 life."
    power = 2
    toughness = 4
    keywords(Keyword.DEATHTOUCH)

    // "attacks the player with the most life" — DefenderIsPlayer restricts to a direct player attack
    // (not a planeswalker); the intervening-if then checks that attacked player has the most life.
    triggeredAbility {
        trigger = Triggers.attacks(requires = setOf(AttackPredicate.DefenderIsPlayer))
        triggerCondition = Conditions.PlayerHasMostLife(Player.DefendingPlayer)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Vampire"),
            keywords = setOf(Keyword.LIFELINK),
            imageUri = "https://cards.scryfall.io/normal/front/0/4/0484390e-1167-4407-84de-7ddc726e8926.jpg?1783913608"
        )
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        triggerCondition = Conditions.PlayerHasMostLife(Player.You)
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "113"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89345f55-2b32-4356-945a-d56dded39909.jpg?1782694521"
    }
}
