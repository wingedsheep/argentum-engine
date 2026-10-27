package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Iroas, God of Victory
 * {2}{R}{W}
 * Legendary Enchantment Creature — God
 * 7/4
 *
 * Indestructible
 * As long as your devotion to red and white is less than seven, Iroas isn't a creature.
 * Creatures you control have menace.
 * Prevent all damage that would be dealt to attacking creatures you control.
 *
 * The god clause is Nylea's Layer 4 [RemoveCardType] gated on a two-colour devotion count
 * (a hybrid symbol of both colours counts once — `devotionTo(RED, WHITE)`). The prevention is
 * Dolmen Gate's live-filter [PreventDamage] shield without the combat-only restriction: per the
 * ruling it stops *all* damage to your attacking creatures, burn included.
 */
val IroasGodOfVictory = card("Iroas, God of Victory") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Enchantment Creature — God"
    power = 7
    toughness = 4
    oracleText = "Indestructible\n" +
        "As long as your devotion to red and white is less than seven, Iroas isn't a creature.\n" +
        "Creatures you control have menace. (They can't be blocked except by two or more creatures.)\n" +
        "Prevent all damage that would be dealt to attacking creatures you control."

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        condition = Conditions.CompareAmounts(
            DynamicAmounts.devotionTo(Color.RED, Color.WHITE),
            ComparisonOperator.LT,
            7
        )
        ability = RemoveCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        ability = GrantKeyword(Keyword.MENACE, GroupFilter.AllCreaturesYouControl)
    }

    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.Object(
                    GameObjectFilter.Creature.youControl().attacking()
                )
            )
        )
    )

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "150"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/cacf28cb-feea-4afb-b22e-1b8d50ee66c1.jpg?1783939404"
        ruling(
            "2014-04-26",
            "Iroas's last ability prevents all damage dealt to attacking creatures you control, not just combat damage."
        )
        ruling(
            "2020-01-24",
            "Your devotion to two colors is the number of mana symbols among mana costs of permanents you control " +
                "that are the first color, the second, or both. If an effect counts your devotion to two colors, a " +
                "hybrid symbol that is both of those colors is counted just once."
        )
        ruling(
            "2020-01-24",
            "If a God stops being a creature, it loses the type creature and the creature type God. It continues " +
                "to be a legendary enchantment."
        )
        ruling(
            "2020-01-24",
            "The type-changing ability that can make a God not be a creature functions only on the battlefield. " +
                "It's always a creature card in other zones, regardless of your devotion to its color. It's always " +
                "a creature spell while it's on the stack."
        )
        ruling(
            "2020-01-24",
            "If a God is attacking or blocking and it stops being a creature, it will be removed from combat. It " +
                "won't rejoin combat if it resumes being a creature later during that combat."
        )
    }
}
