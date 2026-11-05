package com.wingedsheep.mtg.sets.definitions.rav.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ActivatedAbilityScope
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Faith's Fetters
 * {3}{W}
 * Enchantment — Aura
 *
 * Enchant permanent
 * When this Aura enters, you gain 4 life.
 * Enchanted permanent can't attack or block, and its activated abilities can't be activated
 * unless they're mana abilities.
 *
 * Arrest's three-static shape widened one step in each direction: it enchants any *permanent*
 * rather than a creature, and the activation lock spares mana abilities. Both are expressible
 * as-is — [GroupFilter.attachedCreature] is scope-based (`GameObjectFilter.Permanent` scoped to
 * the attachment) despite the name, so it already covers a non-creature host, and
 * [PreventActivatedAbilities] carries `abilities = NON_MANA` for the
 * "… unless they're mana abilities" wording.
 *
 * The combat halves are inert on a non-creature host rather than wrong: a Fettered land was
 * never going to attack or block, and if it later animates the statics are already on it.
 */
val FaithsFetters = card("Faith's Fetters") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant permanent\n" +
        "When this Aura enters, you gain 4 life.\n" +
        "Enchanted permanent can't attack or block, and its activated abilities can't be " +
        "activated unless they're mana abilities."

    auraTarget = TargetObject(filter = TargetFilter.Permanent)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(4)
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
            abilities = ActivatedAbilityScope.NON_MANA,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "16"
        artist = "Chippy"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b8ffba3-44a9-41ce-a5a1-37413346db2f.jpg?1783943702"
        ruling(
            "2020-11-10",
            "If the target permanent is an illegal target by the time Faith's Fetters tries to " +
                "resolve, it doesn't resolve. It won't enter the battlefield, so its " +
                "enters-the-battlefield ability won't trigger."
        )
        ruling(
            "2020-11-10",
            "Faith's Fetters doesn't stop static abilities, triggered abilities, or mana " +
                "abilities from working. A mana ability is an ability that produces mana, not " +
                "an ability that costs mana."
        )
    }
}
