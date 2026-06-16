package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lazav, Familiar Stranger
 * {1}{U}{B}
 * Legendary Creature — Shapeshifter
 * 1/4
 *
 * Whenever you commit a crime, put a +1/+1 counter on Lazav. Then you may exile a card from a
 * graveyard. If a creature card was exiled this way, you may have Lazav become a copy of that card
 * until end of turn. This ability triggers only once each turn.
 *
 * Implementation (inline [Effects.Pipeline], `oncePerTurn = true`):
 *  1. [Effects.AddCounters] a +1/+1 counter on Lazav ([EffectTarget.Self]).
 *  2. "You may exile a card from a graveyard" — gather every card in every graveyard
 *     ([CardSource.FromZone] with [Player.Each]) and let the controller pick up to one to exile.
 *     Declining (picking none) is the "may".
 *  3. Filter the exiled pick to creature cards. If a creature card was exiled, the controller may
 *     have Lazav become a copy of that card until end of turn —
 *     [Effects.EachPermanentBecomesCopyOfTarget] with `affected = Self`, `sourceFromAnyZone = true`
 *     (the copy source sits in exile, not on the battlefield), wrapped in [MayEffect] and gated on
 *     the creature filter via [ConditionalEffect]. Copies copiable values only (Rule 707), so
 *     Lazav keeps his counters and stays a Shapeshifter creature.
 */
val LazavFamiliarStranger = card("Lazav, Familiar Stranger") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Shapeshifter"
    power = 1
    toughness = 4
    oracleText = "Whenever you commit a crime, put a +1/+1 counter on Lazav. Then you may exile a " +
        "card from a graveyard. If a creature card was exiled this way, you may have Lazav become a " +
        "copy of that card until end of turn. This ability triggers only once each turn. (Targeting " +
        "opponents, anything they control, and/or cards in their graveyards is a crime.)"

    triggeredAbility {
        trigger = Triggers.YouCommitCrime
        oncePerTurn = true
        effect = Effects.Pipeline {
            // Put a +1/+1 counter on Lazav.
            run(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self))

            // Then you may exile a card from a graveyard.
            val graveyardCards = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.Each, GameObjectFilter.Any)
            )
            val exiled = chooseUpTo(
                1,
                from = graveyardCards,
                useTargetingUI = true,
                prompt = "You may exile a card from a graveyard",
                selectedLabel = "Exile",
                name = "lazavExiled",
            )
            exile(exiled)

            // If a creature card was exiled this way, you may have Lazav become a copy of that card.
            val creatureExiled = filter(exiled, GameObjectFilter.Creature, name = "lazavCreature")
            run(
                ConditionalEffect(
                    condition = whenMatches(creatureExiled, GameObjectFilter.Creature),
                    effect = MayEffect(
                        Effects.EachPermanentBecomesCopyOfTarget(
                            target = EffectTarget.PipelineTarget(creatureExiled.key),
                            duration = Duration.EndOfTurn,
                            affected = EffectTarget.Self,
                            sourceFromAnyZone = true,
                        ),
                        descriptionOverride = "You may have Lazav become a copy of that card until end of turn.",
                    ),
                )
            )
        }
        description = "Whenever you commit a crime, put a +1/+1 counter on Lazav. Then you may " +
            "exile a card from a graveyard. If a creature card was exiled this way, you may have " +
            "Lazav become a copy of that card until end of turn. This ability triggers only once each turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "216"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/00293326-3eb2-492c-b565-7abafa037d8c.jpg?1712356143"

        ruling("2024-04-12", "Lazav's ability triggers only once each turn, the first time you commit a crime that turn. Committing additional crimes that turn won't cause it to trigger again.")
        ruling("2024-04-12", "You exile the card from a graveyard and have Lazav become a copy of it all while the ability is resolving.")
        ruling("2024-04-12", "Lazav copies the printed values of the exiled creature card (as modified by any copy effects), plus any other copy effects. It keeps its +1/+1 counters and any other counters on it.")
        ruling("2024-04-12", "The copy effect lasts until end of turn. After that, Lazav reverts to being Lazav, Familiar Stranger.")
    }
}
