package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Blastoneuron
 * {3}{B}
 * Creature — Jellyfish Leech
 * 2/2
 *
 * Flying
 * When this creature enters, exile target creature card with mana value 3 or less from your
 * graveyard. Create a token that's a copy of it. That token gains haste. Sacrifice it at the
 * beginning of the next end step.
 * Whenever one or more cards leave your graveyard, each opponent loses 1 life and you gain 1 life.
 *
 * The enters trigger is Nahiri, the Unforgiving's exile-then-copy shape: the token copies the
 * exiled card, and `sacrificeAtStep = Step.END` arms the "next end step" (any player's) delayed
 * sacrifice. Exiling the card is itself a card leaving your graveyard, so the drain trigger fires
 * off Blastoneuron's own enters ability.
 */
val Blastoneuron = card("Blastoneuron") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Jellyfish Leech"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "When this creature enters, exile target creature card with mana value 3 or less from your graveyard. " +
        "Create a token that's a copy of it. That token gains haste. Sacrifice it at the beginning of the next end step.\n" +
        "Whenever one or more cards leave your graveyard, each opponent loses 1 life and you gain 1 life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val reanimated = target(
            TargetFilter(GameObjectFilter.Creature.manaValueAtMost(3).ownedByYou(), zone = Zone.GRAVEYARD)
        )
        effect = Effects.Exile(reanimated) then
            Effects.CreateTokenCopyOfTarget(
                target = reanimated,
                addedKeywords = setOf(Keyword.HASTE),
                sacrificeAtStep = Step.END
            )
    }

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Any).leaveYourGraveyard()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Bastien L. Deharme"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1855a4c-c7d1-407c-b134-4c43968cbdd3.jpg?1792753729"
    }
}
