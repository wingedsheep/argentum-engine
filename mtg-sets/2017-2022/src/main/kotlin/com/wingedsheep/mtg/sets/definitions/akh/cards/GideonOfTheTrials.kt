package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCantLoseGame
import com.wingedsheep.sdk.scripting.GrantOpponentsCantWinGame
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gideon of the Trials
 * {1}{W}{W}
 * Legendary Planeswalker — Gideon
 * Loyalty 3
 *
 * +1: Until your next turn, prevent all damage target permanent would deal.
 * 0: Until end of turn, Gideon becomes a 4/4 Human Soldier creature with indestructible that's
 *    still a planeswalker. Prevent all damage that would be dealt to him this turn.
 * 0: You get an emblem with "As long as you control a Gideon planeswalker, you can't lose the game
 *    and your opponents can't win the game."
 *
 * The +1 is Dovin, Hand of Control's "prevent all damage target permanent would deal" without the
 * damage-to-it half. The first 0 is the Gideon, Champion of Justice shape (creature subtypes added,
 * planeswalker kept). The emblem holds the same two player-level statics Platinum Angel prints,
 * each behind the "you control a Gideon planeswalker" gate; the loss and win checks read emblems
 * and re-evaluate that gate every time, so the lock switches on and off as Gideons come and go.
 */
val GideonOfTheTrials = card("Gideon of the Trials") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker — Gideon"
    startingLoyalty = 3
    oracleText = "+1: Until your next turn, prevent all damage target permanent would deal.\n" +
        "0: Until end of turn, Gideon becomes a 4/4 Human Soldier creature with indestructible that's " +
        "still a planeswalker. Prevent all damage that would be dealt to him this turn.\n" +
        "0: You get an emblem with \"As long as you control a Gideon planeswalker, you can't lose the " +
        "game and your opponents can't win the game.\""

    // +1: Until your next turn, prevent all damage target permanent would deal.
    loyaltyAbility(+1) {
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.PreventAllDamageDealtBy(permanent, duration = Duration.UntilYourNextTurn)
    }

    // 0: Until end of turn, Gideon becomes a 4/4 Human Soldier creature with indestructible that's
    //    still a planeswalker. Prevent all damage that would be dealt to him this turn.
    loyaltyAbility(0) {
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 4,
            toughness = 4,
            keywords = setOf(Keyword.INDESTRUCTIBLE),
            creatureTypes = setOf("Human", "Soldier"),
            duration = Duration.EndOfTurn
        ) then Effects.PreventDamage(target = EffectTarget.Self)
    }

    // 0: You get an emblem with "As long as you control a Gideon planeswalker, you can't lose the
    //    game and your opponents can't win the game."
    loyaltyAbility(0) {
        val controlGideon = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Gideon"))
        effect = Effects.CreatePermanentEmblem(
            ownedStaticAbilities = listOf(
                ConditionalStaticAbility(GrantCantLoseGame, controlGideon),
                ConditionalStaticAbility(GrantOpponentsCantWinGame, controlGideon),
            ),
            emblemDescription = "As long as you control a Gideon planeswalker, you can't lose the game " +
                "and your opponents can't win the game."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "14"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/959ce13f-519f-4472-bbd1-f26a972723d7.jpg?1783936539"
        ruling("2017-04-18", "Gideon's first ability will continue to prevent damage dealt by the target permanent even if Gideon leaves the battlefield before your next turn.")
        ruling("2017-04-18", "Gideon's second ability doesn't count as a creature entering the battlefield. Gideon was already on the battlefield; he only changed his types.")
        ruling("2017-04-18", "If Gideon becomes a creature the same turn he enters the battlefield, you can't attack with him or use any of his {T} abilities (if he gains any).")
        ruling("2017-04-18", "If damage that can't be prevented is dealt to Gideon after his second ability has resolved, that damage will have all applicable results: specifically, the damage is marked on Gideon (since he's a creature) and that damage causes that many loyalty counters to be removed from him (since he's a planeswalker). Even though he has indestructible, if Gideon has no loyalty counters on him, he's put into his owner's graveyard.")
        ruling("2017-04-18", "If Gideon leaves the battlefield after giving you his emblem, you'll keep the emblem. Its effect will apply again later if another Gideon comes under your control.")
        ruling("2017-04-18", "No game effect can cause you to lose the game or cause any opponent to win the game while Gideon's emblem is in effect. It doesn't matter whether you have 0 or less life, you're forced to draw a card while your library is empty, you have ten or more poison counters, you're at your Glorious End, your opponent casts a second Approach of the Second Sun, or so on. You keep playing.")
        ruling("2017-04-18", "If you have Gideon's emblem in a Two-Headed Giant game and control a Gideon planeswalker, your team can't lose the game and the opposing team can't win the game. If your teammate controls a Gideon planeswalker but you do not, your emblem's effect doesn't apply.")
    }
}
