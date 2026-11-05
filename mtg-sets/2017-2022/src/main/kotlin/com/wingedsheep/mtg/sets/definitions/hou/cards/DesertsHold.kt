package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Desert's Hold — Hour of Devastation #8 (canonical printing)
 * {2}{W} · Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, if you control a Desert or there is a Desert card in your graveyard,
 * you gain 3 life.
 * Enchanted creature can't attack or block, and its activated abilities can't be activated.
 *
 * Arrest's three-static lock plus Wretched Camel's Desert intervening-if (CR 603.4): checked as
 * the trigger fires and again on resolution, either half satisfying each check. The activation
 * lock covers mana abilities too — there is no "unless they're mana abilities" clause.
 */
val DesertsHold = card("Desert's Hold") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, if you control a Desert or there is a Desert card in your graveyard, " +
        "you gain 3 life.\n" +
        "Enchanted creature can't attack or block, and its activated abilities can't be activated."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.Any(
            Conditions.ControlPermanentOfType(Subtype.DESERT),
            Conditions.GraveyardContainsSubtype(Subtype.DESERT),
        )
        effect = Effects.GainLife(3)
    }

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Permanent.attachedToBySource(),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "8"
        artist = "Sara Winters"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/8082298e-05c4-48b3-9367-713f59aafdc6.jpg?1783936067"
        ruling("2017-07-14", "Activated abilities contain a colon. They're generally written \"[Cost]: [Effect].\" Some keyword abilities are activated abilities and will have colons in their reminder text. Triggered abilities (starting with \"when,\" \"whenever,\" or \"at\") are unaffected by the last ability of Desert's Hold.")
        ruling("2017-07-14", "If an ability checks whether you control a Desert or there is a Desert card in your graveyard, having more than one doesn't matter. Controlling one is the same as controlling five. There is also no extra bonus for both controlling one and having one in your graveyard.")
        ruling("2017-07-14", "For abilities that trigger only if you control a Desert or there is a Desert card in your graveyard, one condition must be true as the ability triggers and one must be true as the ability resolves. They don't have to be the same condition, though. For example, you could sacrifice your only Desert after the ability triggers but before it has resolved.")
    }
}
