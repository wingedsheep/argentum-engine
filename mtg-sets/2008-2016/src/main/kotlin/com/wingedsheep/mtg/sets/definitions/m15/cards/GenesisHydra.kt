package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Genesis Hydra — Magic 2015 #176 (canonical printing)
 * {X}{G}{G} · Creature — Plant Hydra · 0/0
 *
 * When you cast this spell, reveal the top X cards of your library. You may put a nonland
 * permanent card with mana value X or less from among them onto the battlefield. Then shuffle the
 * rest into your library.
 * This creature enters with X +1/+1 counters on it.
 *
 * The cast trigger reads the spell's announced X through
 * [DynamicAmounts.xValueOfTriggeringSpell], frozen in the trigger context as the spell was cast —
 * so the trigger, which resolves before the Hydra, keeps that X even if the Hydra is countered in
 * response (Old Stickfingers pattern). Revealing doesn't move cards, so the unchosen ones never
 * left the library and the shuffle alone "shuffles the rest in"; it happens even when nothing is
 * left over. The enters-with-counters replacement reads the same cast-time X off the entering
 * permanent ([DynamicAmounts.castX]).
 */
val GenesisHydra = card("Genesis Hydra") {
    manaCost = "{X}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Hydra"
    power = 0
    toughness = 0
    oracleText = "When you cast this spell, reveal the top X cards of your library. You may put a nonland " +
        "permanent card with mana value X or less from among them onto the battlefield. Then shuffle the " +
        "rest into your library.\nThis creature enters with X +1/+1 counters on it."

    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Effects.Pipeline {
            val x = DynamicAmounts.xValueOfTriggeringSpell()
            val revealed = gather(CardSource.TopOfLibrary(x), revealed = true)
            val chosen = chooseUpTo(
                count = 1,
                from = revealed,
                filter = GameObjectFilter.NonlandPermanent.manaValueAtMostDynamic(x),
                prompt = "You may put a nonland permanent card with mana value X or less onto the battlefield"
            )
            move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            run(Effects.ShuffleLibrary())
        }
        description = "When you cast this spell, reveal the top X cards of your library. You may put a " +
            "nonland permanent card with mana value X or less from among them onto the battlefield. " +
            "Then shuffle the rest into your library."
    }

    // "This creature enters with X +1/+1 counters on it."
    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.castX()))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "176"
        artist = "Peter Mohrbacher"
        flavorText = "Designed by George Fan"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/659eb716-bf21-4c0f-9a9a-906236ef536c.jpg?1783939166"
        ruling("2017-11-17", "Genesis Hydra's first ability will resolve before Genesis Hydra does. Notably, if you put an Aura card onto the battlefield this way, it can't enchant Genesis Hydra.")
        ruling("2017-11-17", "If you have fewer than X cards in your library, you reveal all of them.")
        ruling("2017-11-17", "If a card in a player's library has {X} in its mana cost, X is considered to be 0.")
        ruling("2017-11-17", "If \"the rest\" is zero cards, either because X was 0 or because X was 1 and that card was put onto the battlefield, the library is still shuffled.")
    }
}
