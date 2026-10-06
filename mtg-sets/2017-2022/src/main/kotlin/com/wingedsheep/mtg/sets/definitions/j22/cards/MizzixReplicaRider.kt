package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mizzix, Replica Rider — Jumpstart 2022 #35.
 *
 * The trigger is the negated cast-source predicate (casts from any zone other than the hand —
 * exile, graveyard, top of library, …). The copy is the triggering spell itself; a copy of a
 * permanent spell becomes a token as it resolves (CR 707.10f), and the `addedTokenKeywords` /
 * `sacrificeTokenAtStep` riders on [Effects.CopyTargetSpell] give that token haste and the
 * "at the beginning of your end step, sacrifice" clause (gated to the controller's own end step).
 * Both riders are ignored for a non-permanent copy, matching "If the copy is a permanent spell".
 */
val MizzixReplicaRider = card("Mizzix, Replica Rider") {
    manaCost = "{4}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Goblin Wizard"
    oracleText = "Flying\n" +
        "Whenever you cast a spell from anywhere other than your hand, you may pay {1}{U/R}. If you do, " +
        "copy that spell and you may choose new targets for the copy. If the copy is a permanent spell, " +
        "it gains haste and \"At the beginning of your end step, sacrifice this permanent.\" " +
        "(A copy of a permanent spell becomes a token.)"
    power = 4
    toughness = 5

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.CastFromZoneOtherThan(Zone.HAND)))
        effect = Effects.MayPay(
            cost = ManaCost.parse("{1}{U/R}"),
            then = Effects.CopyTargetSpell(
                target = EffectTarget.TriggeringEntity,
                addedTokenKeywords = setOf(Keyword.HASTE),
                sacrificeTokenAtStep = Step.END,
                sacrificeTokenOnlyOnControllersTurn = true
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "35"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8faf1d83-3b6b-4e07-a024-3dfd37c29814.jpg?1783919182"
        ruling("2022-12-02", "Any spell you cast will be copied if you pay the cost, not just one that requires targets.")
        ruling("2022-12-02", "You may only pay the cost once, as the triggered ability resolves. You can't pay it more than once to get additional copies.")
        ruling("2022-12-02", "If a copy is created, you control the copy. That copy is created on the stack, so it's not \"cast.\" Abilities that trigger when a player casts a spell won't trigger.")
        ruling("2022-12-02", "You can't choose to pay any additional costs for the copy. However, effects based on any additional costs that were paid for the original spell are copied as though those same costs were paid for the copy too.")
    }
}
