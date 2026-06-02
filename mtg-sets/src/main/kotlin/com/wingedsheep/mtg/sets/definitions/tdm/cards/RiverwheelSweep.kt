package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Riverwheel Sweep
 * {2/U}{2/R}{2/W}
 * Sorcery
 *
 * Tap target creature. Put three stun counters on it.
 * (If a permanent with a stun counter would become untapped, remove one from it instead.)
 * Exile the top two cards of your library. Choose one of them. Until the end of your next
 * turn, you may play that card.
 *
 * The impulse half composes the standard Gather → Move(EXILE) → Select(one) → grant
 * may-play pipeline (see Fireglass Mentor); the only difference is the
 * [MayPlayExpiry.UntilEndOfNextTurn] window. The other exiled card stays exiled with no
 * play permission, matching "Choose one of them."
 */
val RiverwheelSweep = card("Riverwheel Sweep") {
    manaCost = "{2/U}{2/R}{2/W}"
    colorIdentity = "URW"
    typeLine = "Sorcery"
    oracleText = "Tap target creature. Put three stun counters on it. " +
        "(If a permanent with a stun counter would become untapped, remove one from it instead.)\n" +
        "Exile the top two cards of your library. Choose one of them. Until the end of your next turn, " +
        "you may play that card."

    spell {
        val creature = target("target creature", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.Tap(creature)
            .then(Effects.AddCounters(Counters.STUN, 3, creature))
            .then(
                Effects.Composite(
                    listOf(
                        GatherCardsEffect(
                            source = CardSource.TopOfLibrary(DynamicAmount.Fixed(2)),
                            storeAs = "exiled"
                        ),
                        MoveCollectionEffect(
                            from = "exiled",
                            destination = CardDestination.ToZone(Zone.EXILE)
                        ),
                        SelectFromCollectionEffect(
                            from = "exiled",
                            selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                            storeSelected = "chosen",
                            prompt = "Choose a card you may play until the end of your next turn"
                        ),
                        GrantMayPlayFromExileEffect(
                            from = "chosen",
                            expiry = MayPlayExpiry.UntilEndOfNextTurn
                        )
                    )
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "219"
        artist = "Wayne Wu"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/686fe623-ee50-407d-87c9-664fb039f4d9.jpg?1743204865"
    }
}
