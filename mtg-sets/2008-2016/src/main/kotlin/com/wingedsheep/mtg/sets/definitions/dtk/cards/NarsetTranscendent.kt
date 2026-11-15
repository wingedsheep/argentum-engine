package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Narset Transcendent — Dragons of Tarkir #225
 * {2}{W}{U} · Legendary Planeswalker — Narset · Starting loyalty 6
 *
 * +1: Look at the top card of your library. If it's a noncreature, nonland card, you may reveal it
 *     and put it into your hand.
 * −2: When you next cast an instant or sorcery spell from your hand this turn, it gains rebound.
 * −9: You get an emblem with "Your opponents can't cast noncreature spells."
 *
 * The +1 is the look-at-top peek pipeline (Traveling Botanist's shape): a card that isn't a
 * noncreature, nonland card, or one the player declines, stays on top unrevealed (DTK ruling).
 * The −2 is a one-shot end-of-turn delayed cast trigger stamping rebound on the triggering spell —
 * Ojer Pakpatiq's grant, fired once. The −9 emblem owns a [PlayersCantCastSpells] static, which the
 * cast-legality chokepoint reads off emblems beside the battlefield.
 */
val NarsetTranscendent = card("Narset Transcendent") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Planeswalker — Narset"
    startingLoyalty = 6
    oracleText = "+1: Look at the top card of your library. If it's a noncreature, nonland card, you " +
        "may reveal it and put it into your hand.\n" +
        "−2: When you next cast an instant or sorcery spell from your hand this turn, it gains " +
        "rebound. (Exile the spell as it resolves. At the beginning of your next upkeep, you may " +
        "cast that card from exile without paying its mana cost.)\n" +
        "−9: You get an emblem with \"Your opponents can't cast noncreature spells.\""

    loyaltyAbility(+1) {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(1))
            val eligible = filter(looked, GameObjectFilter.Noncreature and GameObjectFilter.Nonland)
            val toHand = chooseUpTo(
                1,
                from = eligible,
                selectedLabel = "Reveal and put into your hand",
                remainderLabel = "Leave on top of your library"
            )
            move(toHand, CardDestination.ToZone(Zone.HAND), revealed = true, revealToSelf = false)
        }
        description = "Look at the top card of your library. If it's a noncreature, nonland card, you " +
            "may reveal it and put it into your hand."
    }

    loyaltyAbility(-2) {
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.you.casts(
                GameObjectFilter.InstantOrSorcery,
                requires = setOf(SpellCastPredicate.CastFromZone(Zone.HAND))
            ),
            fireOnce = true,
            expiry = DelayedTriggerExpiry.EndOfTurn,
            effect = Effects.GrantKeywordToSpell(Keyword.REBOUND, EffectTarget.TriggeringEntity)
        )
        description = "When you next cast an instant or sorcery spell from your hand this turn, it " +
            "gains rebound."
    }

    loyaltyAbility(-9) {
        effect = Effects.CreatePermanentEmblem(
            ownedStaticAbilities = listOf(
                PlayersCantCastSpells(affected = Player.EachOpponent, spellFilter = GameObjectFilter.Noncreature)
            ),
            emblemDescription = "Your opponents can't cast noncreature spells."
        )
        description = "You get an emblem with \"Your opponents can't cast noncreature spells.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "225"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/4816d092-e34b-4397-8599-43d8e85a8f39.jpg?1783938571"

        ruling("2015-02-25", "When resolving Narset's first ability, if the top card of your library is a creature or a land card, or if you don't wish to put it into your hand, it isn't revealed.")
        ruling("2015-02-25", "If a spell that exiles itself as part of its own resolution, such as Volcanic Vision, gains rebound, the ability that lets you cast the spell again won't be created. The spell will simply be exiled.")
    }
}
