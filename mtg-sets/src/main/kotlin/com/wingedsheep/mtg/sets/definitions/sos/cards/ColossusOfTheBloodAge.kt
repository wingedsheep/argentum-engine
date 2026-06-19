package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Colossus of the Blood Age — Secrets of Strixhaven #181
 * {4}{R}{W} · Artifact Creature — Construct · 6/6
 *
 * When this creature enters, it deals 3 damage to each opponent and you gain 3 life.
 * When this creature dies, discard any number of cards, then draw that many cards plus one.
 *
 * The ETB ability is a composite of [Effects.DealDamage] to each opponent (the source is
 * the construct via `damageSource = EffectTarget.Self`) and a 3-life gain. The death
 * ability is an inline Gather → Select(any number) → Discard → Draw pipeline over the
 * controller's own hand: the draw count reads the discarded collection's count plus one
 * ([DynamicAmount.Add]). Discarding zero is legal — you then draw exactly one card.
 */
val ColossusOfTheBloodAge = card("Colossus of the Blood Age") {
    manaCost = "{4}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Artifact Creature — Construct"
    power = 6
    toughness = 6
    oracleText = "When this creature enters, it deals 3 damage to each opponent and you gain 3 life.\n" +
        "When this creature dies, discard any number of cards, then draw that many cards plus one."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        // No explicit damageSource: the engine attributes the damage to the ability's source
        // (Colossus itself) by default, matching the "it deals damage" self-source convention.
        effect = Effects.Composite(
            Effects.DealDamage(3, EffectTarget.PlayerRef(Player.EachOpponent)),
            Effects.GainLife(3)
        )
    }

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
            val discarded = chooseAnyNumber(
                from = hand,
                prompt = "Choose any number of cards to discard"
            )
            move(
                discarded,
                CardDestination.ToZone(Zone.GRAVEYARD, Player.You),
                moveType = MoveType.Discard
            )
            run(
                DrawCardsEffect(
                    DynamicAmount.Add(
                        DynamicAmount.VariableReference("${discarded.key}_count"),
                        DynamicAmount.Fixed(1)
                    ),
                    EffectTarget.Controller
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "181"
        artist = "Leon Tukker"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bfa7f0a4-6b65-4e53-ba00-848df260d8e3.jpg?1775938248"
    }
}
