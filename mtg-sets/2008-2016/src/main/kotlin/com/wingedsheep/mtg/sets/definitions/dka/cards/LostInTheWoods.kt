package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lost in the Woods — Dark Ascension #123
 * {3}{G}{G} · Enchantment
 *
 * Whenever a creature attacks you or a planeswalker you control, reveal the top card of your
 * library. If it's a Forest card, remove that creature from combat. Then put the revealed card on
 * the bottom of your library.
 *
 * An ANY-binding attack trigger fires once per declared attacker, each bound to its own attacker
 * (the triggering entity) before any card is revealed (2011-01-22 ruling). "Forest card" is any
 * card with the Forest land subtype, basic or not.
 */
val LostInTheWoods = card("Lost in the Woods") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature attacks you or a planeswalker you control, reveal the top card of " +
        "your library. If it's a Forest card, remove that creature from combat. Then put the revealed card " +
        "on the bottom of your library."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.attackingYouOrYourPlaneswalkers()).attacks()
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(1, Player.You), revealed = true)
            run(Effects.If(
                condition = whenMatches(revealed, GameObjectFilter.Land.withSubtype("Forest")),
                then = Effects.RemoveFromCombat(EffectTarget.TriggeringEntity),
            ))
            move(
                revealed,
                CardDestination.ToZone(Zone.LIBRARY, Player.You, placement = ZonePlacement.Bottom),
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "123"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/5865603c-0a5e-45c3-84e3-2dc3b4cf0cf7.jpg?1783940803"
        ruling("2011-01-22", "Removing an attacking creature from combat doesn't untap that creature.")
        ruling(
            "2011-01-22",
            "Because Lost in the Woods will trigger once for each creature that attacks you or a planeswalker " +
                "you control, it's important to specify which trigger is associated with which attacking " +
                "creature before revealing any cards."
        )
    }
}
