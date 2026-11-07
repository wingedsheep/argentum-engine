package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

private const val SOLDIER_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/d/f/dfb0ae06-9a02-4cd6-bc1d-423662d7a1bd.jpg?1783903464"

private fun soldierToken() = Effects.CreateToken(
    power = 1,
    toughness = 1,
    colors = setOf(Color.RED),
    creatureTypes = setOf("Soldier"),
    keywords = setOf(Keyword.HASTE),
    imageUri = SOLDIER_TOKEN_IMAGE
)

/**
 * Frontline Heroism — Foundations Jumpstart #15
 * {2}{R} · Enchantment
 *
 * When this enchantment enters, create a 1/1 red Soldier creature token with haste.
 * Whenever you cast a spell that targets only a single creature you control, create a 1/1 red
 * Soldier creature token with haste, then copy that spell. The copy targets that token.
 *
 * "A spell" is any spell — an Aura included, whose copy becomes a token Aura on the Soldier. The
 * copy is CR 707.10e's specified new target (the rule's own example is this card): no copy when the
 * token isn't a legal target for every instance of "target", and with a token doubler the copy
 * targets the one token you choose, never both.
 */
val FrontlineHeroism = card("Frontline Heroism") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, create a 1/1 red Soldier creature token with haste.\n" +
        "Whenever you cast a spell that targets only a single creature you control, create a 1/1 red " +
        "Soldier creature token with haste, then copy that spell. The copy targets that token."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = soldierToken()
        description = "When this enchantment enters, create a 1/1 red Soldier creature token with haste."
    }

    triggeredAbility {
        trigger = Triggers.you.casts(
            requires = setOf(
                SpellCastPredicate.TargetsMatching(GameObjectFilter.Creature.youControl(), only = true)
            )
        )
        effect = soldierToken() then Effects.CopyTargetingOneOf(CollectionSlot.CreatedTokens)
        description = "Whenever you cast a spell that targets only a single creature you control, create a " +
            "1/1 red Soldier creature token with haste, then copy that spell. The copy targets that token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Fajareka Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e78eda51-255a-4421-9b6f-025a432e9df9.jpg?1783908866"
        ruling("2024-11-08", "Frontline Heroism's last ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered or otherwise leaves the stack without resolving.")
        ruling("2024-11-08", "If you cast a spell that has multiple targets but all of them are the same creature you control, Frontline Heroism's last ability will trigger. In that case, the copy's targets will all be the token created by the ability.")
        ruling("2024-11-08", "The copy created by Frontline Heroism's last ability is created on the stack, so it's not \"cast\". Abilities that trigger when a player casts a spell (like Frontline Heroism's own ability) won't trigger.")
        ruling("2024-11-08", "If the spell that's copied is modal (that is, it says \"Choose one —\" or the like), the copy will have the same mode or modes. You can't choose different ones.")
        ruling("2024-11-08", "If the spell that's copied has an X whose value was determined as it was cast, the copy will have the same value of X.")
        ruling("2024-11-08", "You can't choose to pay any additional costs for the copy. However, effects based on any additional costs that were paid for the original spell are copied as though those same costs were paid for the copy too.")
        ruling("2024-11-08", "Any choices made when the spell resolves won't have been made yet when it's copied. Any such choices will be made separately when the copy resolves.")
        ruling("2024-11-08", "If a permanent spell is copied, the copy is put onto the battlefield as a token as the spell resolves rather than putting the copy of the spell onto the battlefield. The rules that apply to a permanent spell becoming a permanent apply to a copy of a spell becoming a token.")
        ruling("2024-11-08", "The token that a resolving copy of a permanent spell becomes isn't \"created\" and won't interact with abilities that care about tokens being created, such as the first ability of Doubling Season.")
    }
}
