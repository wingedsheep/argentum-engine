package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Feral Deathgorger // Dusk Sight — Tarkir: Dragonstorm #80
 * {5}{B} // {1}{B} · Creature — Dragon // Sorcery — Omen · 3/5
 *
 * Feral Deathgorger:
 *   Flying, deathtouch
 *   When this creature enters, exile up to two target cards from a single graveyard.
 *
 * Dusk Sight — {1}{B}, Sorcery — Omen:
 *   Put a +1/+1 counter on up to one target creature. Draw a card.
 *
 * The ETB reuses Arashin Sunshield's "single graveyard" pattern: a [TargetObject] with
 * `sameOwner = true` so both chosen cards come from one player's graveyard, exiled via
 * [ForEachTargetEffect] + [MoveToZoneEffect]. The Omen face is modeled like an Adventure
 * (CR 715 / [com.wingedsheep.sdk.model.CardLayout.OMEN]); on resolution it shuffles this card
 * into its owner's library instead of going to the graveyard.
 */
val FeralDeathgorger = card("Feral Deathgorger") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Dragon"
    power = 3
    toughness = 5
    oracleText = "Flying, deathtouch\n" +
        "When this creature enters, exile up to two target cards from a single graveyard."

    keywords(Keyword.FLYING, Keyword.DEATHTOUCH)

    // ETB: exile up to two target cards, both from the same graveyard (sameOwner).
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target(
            "up to two target cards from a single graveyard",
            TargetObject(
                count = 2,
                optional = true,
                filter = TargetFilter.CardInGraveyard,
                sameOwner = true,
            )
        )
        effect = ForEachTargetEffect(
            effects = listOf(Effects.Move(EffectTarget.ContextTarget(0), Zone.EXILE))
        )
    }

    // Dusk Sight — Omen. Put a +1/+1 counter on up to one target creature. Draw a card.
    omen("Dusk Sight") {
        manaCost = "{1}{B}"
        typeLine = "Sorcery — Omen"
        oracleText = "Put a +1/+1 counter on up to one target creature. Draw a card. " +
            "(Then shuffle this card into its owner's library.)"
        spell {
            val creature = target("creature", Targets.UpToCreatures(1))
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature)
                .then(Effects.DrawCards(1))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "80"
        artist = "Loïc Canavaggia"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a147b94f-dfcf-44ce-8a73-b2fe6c4efc0e.jpg?1743204282"
    }
}
