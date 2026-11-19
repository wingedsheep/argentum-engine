package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mindreaver — Born of the Gods #44 (canonical printing)
 * {U}{U}
 * Creature — Human Wizard
 * 2/1
 * Heroic — Whenever you cast a spell that targets this creature, exile the top three cards of
 * target player's library.
 * {U}{U}, Sacrifice this creature: Counter target spell with the same name as a card exiled with
 * this creature.
 *
 * The heroic exile links every card to Mindreaver's pile (`linkToSource`), and the counter reads
 * that pile whole through `CardPredicate.SharesNameWithLinkedExile` (Circu's predicate). The
 * sacrifice is a cost, so by the time the counter resolves and re-checks its target Mindreaver is
 * in the graveyard: the predicate reads the pile of the battlefield visit the ability came from
 * (CR 607.2a linked abilities), never a later Mindreaver's (the card's second ruling).
 */
val Mindreaver = card("Mindreaver") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 1
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, exile the top three cards of target player's library.\n" +
        "{U}{U}, Sacrifice this creature: Counter target spell with the same name as a card exiled with this creature."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        target(Targets.Player)
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(3, Player.TargetPlayer))
            exile(top, linkToSource = true)
        }
        description = "Heroic — Whenever you cast a spell that targets this creature, exile the top three cards of target player's library."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}{U}"), Costs.SacrificeSelf)
        target(TargetFilter.SpellOnStack.sharingNameWithLinkedExile())
        effect = Effects.CounterSpell()
        description = "{U}{U}, Sacrifice this creature: Counter target spell with the same name as a card exiled with this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "44"
        artist = "Wesley Burt"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87912ae7-3edc-4ea2-9b4b-caf2bc149ad1.jpg?1783939567"
        ruling("2014-02-01", "The cards are exiled face up. All players can look at them.")
        ruling("2014-02-01", "If Mindreaver leaves the battlefield, and later another Mindreaver enters the battlefield, it is a new object (even if the two were represented by the same card). The last ability of the second Mindreaver doesn't refer to any cards exiled with the first one.")
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
