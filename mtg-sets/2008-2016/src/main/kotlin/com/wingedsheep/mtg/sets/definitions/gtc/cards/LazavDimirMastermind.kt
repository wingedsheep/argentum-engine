package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.effects.CopySourceRead
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lazav, Dimir Mastermind — Gatecrash #174 (canonical printing)
 * {U}{U}{B}{B} · Legendary Creature — Shapeshifter · 3/3
 *
 * Hexproof
 * Whenever a creature card is put into an opponent's graveyard from anywhere, you may have Lazav
 * become a copy of that card, except its name is Lazav, Dimir Mastermind, it's legendary in addition
 * to its other types, and it has hexproof and this ability.
 *
 * The trigger is Profane Memento's "from anywhere" zone change (tokens aren't cards). The copy is
 * [CopySourceRead.CARD]: "that card" is the card in the graveyard, not the permanent that died, so a
 * dead Clone makes Lazav a 0/0 Clone rather than whatever it was copying, and a card that has left
 * the graveyard by resolution is still copied as it last existed there (the 2013-01-24 rulings).
 * `retainResolvingTriggeredAbility` is "and this ability"; the copy has no ETB, so none fires.
 */
val LazavDimirMastermind = card("Lazav, Dimir Mastermind") {
    manaCost = "{U}{U}{B}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Shapeshifter"
    power = 3
    toughness = 3
    oracleText = "Hexproof\nWhenever a creature card is put into an opponent's graveyard from anywhere, " +
        "you may have Lazav become a copy of that card, except its name is Lazav, Dimir Mastermind, " +
        "it's legendary in addition to its other types, and it has hexproof and this ability."

    keywords(Keyword.HEXPROOF)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.nontoken().ownedByOpponent())
            .changesZone(to = Zone.GRAVEYARD)
        effect = Effects.May(
            Effects.EachPermanentBecomesCopyOfTarget(
                target = EffectTarget.TriggeringEntity,
                affected = EffectTarget.Self,
                sourceRead = CopySourceRead.CARD,
                exceptions = CopyExceptions(
                    nameOverride = "Lazav, Dimir Mastermind",
                    addedSupertypes = setOf(Supertype.LEGENDARY),
                    addedKeywords = setOf(Keyword.HEXPROOF),
                    retainResolvingTriggeredAbility = true,
                ),
            ),
            sourceRequiredZone = Zone.BATTLEFIELD,
            descriptionOverride = "Have Lazav become a copy of that card?",
        )
        description = "Whenever a creature card is put into an opponent's graveyard from anywhere, " +
            "you may have Lazav become a copy of that card, except its name is Lazav, Dimir Mastermind, " +
            "it's legendary in addition to its other types, and it has hexproof and this ability."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "174"
        artist = "David Rapoza"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69c8fcdb-4798-4961-995a-e128a3ff431a.jpg?1783940106"
        ruling(
            "2013-01-24",
            "Lazav, Dimir Mastermind becomes a copy of the creature card in the graveyard. If that card is " +
                "no longer in the graveyard when Lazav's ability resolves, use the characteristics of that " +
                "card as it last existed in the graveyard. Notably, copy effects that applied to the creature " +
                "card when it was on the battlefield won't be copied by Lazav. For example, if a Clone is put " +
                "into an opponent's graveyard from the battlefield and you use Lazav's ability, it will become " +
                "0/0. It won't become a copy of whatever Clone was copying."
        )
        ruling(
            "2013-01-24",
            "If multiple creature cards are put into an opponent's graveyard at the same time, you choose " +
                "the order that Lazav's triggered abilities go on the stack."
        )
        ruling(
            "2013-01-24",
            "\"Enters\" abilities of the creature card Lazav is copying won't trigger as Lazav is already on " +
                "the battlefield when it becomes a copy of that creature card."
        )
        ruling("2013-01-24", "Token creatures dying won't cause Lazav's triggered ability to trigger.")
    }
}
