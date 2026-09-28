package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rona, Herald of Invasion // Rona, Tolarian Obliterator (March of the Machine #75)
 * {1}{U} Legendary Creature — Human Wizard 1/3 // Legendary Creature — Phyrexian Wizard 5/5
 *
 * Back — "that source's controller" is [Player.ControllerOfTriggeringEntity]; the random exile is
 * from that player's hand. A land may be put onto the battlefield under Rona's controller;
 * anything else may be cast without paying its mana cost.
 */
private val RonaHeraldOfInvasionFront = card("Rona, Herald of Invasion") {
    manaCost = "{1}{U}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Wizard"
    power = 1
    toughness = 3
    oracleText = "Whenever you cast a legendary spell, untap Rona.\n" +
        "{T}: Draw a card, then discard a card.\n" +
        "{5}{B/P}: Transform Rona. Activate only as a sorcery. " +
        "({B/P} can be paid with either {B} or 2 life.)"

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.legendary())
        effect = Effects.Untap(EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Patterns.Hand.loot()
        description = "Draw a card, then discard a card."
    }

    activatedAbility {
        cost = Costs.Mana("{5}{B/P}")
        effect = Effects.Transform(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform Rona."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f487b582-e73f-4325-939f-95fc5a9aba49.jpg?1783917031"
    }
}

private val RonaTolarianObliterator = card("Rona, Tolarian Obliterator") {
    manaCost = ""
    colorIndicator = "UB" // Transformed back face, no mana cost (CR 204).
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Phyrexian Wizard"
    power = 5
    toughness = 5
    oracleText = "Trample\n" +
        "Whenever a source deals damage to Rona, that source's controller exiles a card from their hand " +
        "at random. If it's a land card, you may put it onto the battlefield under your control. " +
        "Otherwise, you may cast it without paying its mana cost."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.ControllerOfTriggeringEntity))
            val exiled = chooseRandom(1, from = hand)
            move(exiled, CardDestination.ToZone(Zone.EXILE))
            run(Effects.If(
                condition = whenMatches(exiled, GameObjectFilter.Land),
                then = Effects.May(Effects.Pipeline {
                    move(exiled, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
                }),
                otherwise = Effects.CastFromCollectionWithoutPayingCost(exiled)
            ))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "Victor Adame Minguez"
        flavorText = "\"At last, I am *compleat*.\""
        imageUri = "https://cards.scryfall.io/normal/back/f/4/f487b582-e73f-4325-939f-95fc5a9aba49.jpg?1783917031"
    }
}

val RonaHeraldOfInvasion: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = RonaHeraldOfInvasionFront,
    backFace = RonaTolarianObliterator,
)
