package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Annie Flash, the Veteran
 * {3}{R}{G}{W}
 * Legendary Creature — Human Rogue
 * 4/5
 *
 * Flash
 * When Annie Flash enters, if you cast it, return target permanent card with mana value 3 or
 * less from your graveyard to the battlefield tapped.
 * Whenever Annie Flash becomes tapped, exile the top two cards of your library. You may play
 * those cards this turn.
 *
 * - The reanimation ETB is gated by [Conditions.WasCast] (intervening-if "if you cast it"), so it
 *   does not trigger when Annie is put onto the battlefield by another effect (per the 2024-04-12
 *   ruling). The target is a single printed "permanent card with mana value 3 or less" you own in
 *   your graveyard ([GameObjectFilter.Permanent].ownedByYou().manaValueAtMost(3)); it enters the
 *   battlefield tapped via [Effects.PutOntoBattlefield] with `tapped = true`.
 * - The "becomes tapped" trigger is the impulse-draw facade [Patterns.Exile.impulse] for two cards
 *   (gather top two → exile → grant "may play until end of turn"), the same shape as Irascible
 *   Wolverine but for two cards. It fires on any tap (attacking, abilities, opponents' effects)
 *   since it has no qualifier.
 */
val AnnieFlashTheVeteran = card("Annie Flash, the Veteran") {
    manaCost = "{3}{R}{G}{W}"
    colorIdentity = "RGW"
    typeLine = "Legendary Creature — Human Rogue"
    power = 4
    toughness = 5
    oracleText = "Flash\n" +
        "When Annie Flash enters, if you cast it, return target permanent card with mana value 3 " +
        "or less from your graveyard to the battlefield tapped.\n" +
        "Whenever Annie Flash becomes tapped, exile the top two cards of your library. You may " +
        "play those cards this turn."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.WasCast
        val card = target(
            "permanent card with mana value 3 or less from your graveyard",
            TargetObject(
                filter = TargetFilter(
                    GameObjectFilter.Permanent.ownedByYou().manaValueAtMost(3),
                    zone = Zone.GRAVEYARD,
                ),
            ),
        )
        effect = Effects.PutOntoBattlefield(card, tapped = true)
        description = "When Annie Flash enters, if you cast it, return target permanent card with " +
            "mana value 3 or less from your graveyard to the battlefield tapped."
    }

    triggeredAbility {
        trigger = Triggers.BecomesTapped
        effect = Patterns.Exile.impulse(2)
        description = "Whenever Annie Flash becomes tapped, exile the top two cards of your " +
            "library. You may play those cards this turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "190"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d4af7c3-a70d-4f71-b27d-b268c4a0f81e.jpg?1712356036"

        ruling("2024-04-12", "Annie Flash's second ability triggers if you cast it from any zone. It doesn't trigger if you put Annie Flash onto the battlefield without casting it.")
        ruling("2024-04-12", "A permanent card is an artifact, battle, creature, enchantment, land, or planeswalker card.")
        ruling("2024-04-12", "If the mana cost of a card in your graveyard includes {X}, X is 0 for the purpose of determining its mana value.")
        ruling("2024-04-12", "You pay all costs and follow all normal timing rules for cards played from exile with Annie Flash's last ability. For example, if the exiled card is a land card, you may play it only during your main phase while the stack is empty.")
    }
}
