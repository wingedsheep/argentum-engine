package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Faith of the Devoted
 * {2}{B}
 * Enchantment
 * Whenever you cycle or discard a card, you may pay {1}. If you do, each opponent loses 2 life and
 * you gain 2 life.
 *
 * "Cycle or discard" is a single discard trigger: cycling discards the card (CR 702.29a), so
 * `Triggers.you.discards()` fires once per cycled card, and wiring `cycles()` as well would
 * double-trigger against the ruling. The gain is a fixed 2 (not "life lost this way"), so it is a
 * plain LoseLife-then-GainLife, as on Kheru Bloodsucker; the {1} is a single optional payment.
 */
val FaithOfTheDevoted = card("Faith of the Devoted") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever you cycle or discard a card, you may pay {1}. If you do, each opponent loses 2 life and you gain 2 life."

    triggeredAbility {
        trigger = Triggers.you.discards()
        effect = Effects.MayPay(
            ManaCost.parse("{1}"),
            Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(2)
        )
        description = "Whenever you cycle or discard a card, you may pay {1}. If you do, each opponent loses 2 life and you gain 2 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "90"
        artist = "Raoul Vitale"
        flavorText = "\"The more I see of this place, the less I want to.\"\n—Chandra Nalaar"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c26e0e0-d157-4d63-bebe-b02d208c5572.jpg?1783936506"
        ruling("2017-04-18", "While resolving the triggered ability of Faith of the Devoted, you can't pay {1} multiple times to multiply the effect.")
        ruling("2017-04-18", "In a Two-Headed Giant game, the triggered ability of Faith of the Devoted causes the opposing team to lose 4 life and you gain 2 life.")
        ruling("2017-04-18", "Some cards have an ability that triggers whenever you cycle any card. These triggered abilities resolve before you draw from the cycling ability.")
        ruling("2017-04-18", "An ability that triggers whenever you discard a card doesn't give you permission to discard cards. You'll need another effect that instructs or allows you to discard them.")
        ruling("2017-04-18", "An ability that triggers whenever you \"cycle or discard\" a card triggers only once if you cycle a card. The ability \"Whenever you discard a card\" is functionally identical to this ability; cycling is mentioned for clarity.")
        ruling("2017-04-18", "If a player discards a card during their cleanup step due to having too many cards in hand, any appropriate abilities that trigger on discarding that card trigger. If this happens, those triggered abilities are put onto the stack and players receive priority in that cleanup step to cast spells or activate abilities (normally, no players may take actions during a cleanup step). Another cleanup step is created following that one.")
    }
}
