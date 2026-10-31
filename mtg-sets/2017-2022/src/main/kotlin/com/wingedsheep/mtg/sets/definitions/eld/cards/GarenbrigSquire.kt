package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Garenbrig Squire — Throne of Eldraine #158
 * {1}{G} · Creature — Human Soldier · 2/2
 *
 * Whenever you cast a creature spell that has an Adventure, this creature gets +1/+1 until end of turn.
 *
 * Same `Creature ∧ HasAdventure` cast filter as Edgewall Innkeeper: an adventurer cast as its Adventure
 * is an instant/sorcery spell on the stack, so it doesn't trigger this.
 */
val GarenbrigSquire = card("Garenbrig Squire") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Whenever you cast a creature spell that has an Adventure, this creature gets +1/+1 until end of turn. " +
        "(It doesn't need to have gone on the adventure first.)"

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature.withCardPredicate(CardPredicate.HasAdventure))
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "158"
        artist = "Dallas Williams"
        flavorText = "\"Find him a weapon as big as his dreams.\"\n—Yorvo, lord of Garenbrig"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74237cce-2ca2-4bfd-a846-c7309621a85f.jpg?1783932610"
        ruling(
            "2019-10-04",
            "Garenbrig Squire's ability resolves before the spell that caused it to trigger. It resolves even " +
                "if that spell is countered."
        )
        ruling(
            "2019-10-04",
            "An effect may refer to a card, spell, or permanent that \"has an Adventure.\" This refers to a " +
                "card, spell, or permanent that has an adventurer card's set of alternative characteristics, " +
                "even if they're not being used and even if that card was never cast as an Adventure."
        )
        ruling(
            "2019-10-04",
            "If an effect refers to a card, spell, or permanent that has an Adventure, it won't find an " +
                "instant or sorcery spell on the stack that's been cast as an Adventure."
        )
    }
}
