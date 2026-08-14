package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.DamagePredicate
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Imodane, the Pyrohammer (WOE #137)
 * {2}{R}{R} Legendary Creature — Human Knight, 4/4
 *
 * Whenever an instant or sorcery spell you control that targets only a single creature deals
 * damage to that creature, Imodane deals that much damage to each opponent.
 *
 * The damage observer restricts the source to an instant or sorcery spell controlled by Imodane's
 * controller and the recipient to a creature. SourceSoleTargetIsRecipient both requires one target
 * and ties that target to this damage recipient, so collateral damage does not trigger Imodane. The
 * damage amount is captured in the trigger context before the spell leaves the stack.
 */
val ImodaneThePyrohammer = card("Imodane, the Pyrohammer") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Knight"
    oracleText = "Whenever an instant or sorcery spell you control that targets only a single " +
        "creature deals damage to that creature, Imodane deals that much damage to each opponent."
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.dealsDamage(
            recipient = RecipientFilter.AnyCreature,
            sourceFilter = GameObjectFilter.InstantOrSorcery.youControl(),
            binding = TriggerBinding.ANY,
            requires = setOf(DamagePredicate.SourceSoleTargetIsRecipient),
        )
        effect = Effects.DealDamage(
            amount = DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT),
            target = EffectTarget.PlayerRef(Player.EachOpponent),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "137"
        artist = "Chris Rahn"
        flavorText = "\"The realm needs a ruler, not a boy playing king.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14b44833-0482-4b47-a594-4050bb87f1a5.jpg?1783915094"
    }
}
