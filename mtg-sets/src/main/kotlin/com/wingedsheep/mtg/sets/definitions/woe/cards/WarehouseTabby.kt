package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Warehouse Tabby
 * {B}
 * Creature — Cat
 * 1/1
 *
 * Whenever an enchantment you control is put into a graveyard from the battlefield, create a
 * 1/1 black Rat creature token with "This token can't block."
 * {1}{B}: This creature gains deathtouch until end of turn.
 *
 * The trigger fires on *any* enchantment you control hitting the graveyard from the battlefield
 * — destroyed, sacrificed, or put there by its own ability (Hopeless Nightmare, the Role tokens
 * that fall off when replaced) — so it uses the generic `leavesBattlefield` factory with an
 * `ANY` binding rather than the SELF-bound named constants.
 */
val WarehouseTabby = card("Warehouse Tabby") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Cat"
    oracleText = "Whenever an enchantment you control is put into a graveyard from the battlefield, " +
        "create a 1/1 black Rat creature token with \"This token can't block.\"\n" +
        "{1}{B}: This creature gains deathtouch until end of turn."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Enchantment.youControl(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY
        )
        effect = woeRatToken()
    }

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "117"
        artist = "Steve Prescott"
        flavorText = "For every rat she kills, ten more take its place."
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d500ad81-9659-4a00-8f99-8be7c23587e8.jpg?1783915100"
    }
}
