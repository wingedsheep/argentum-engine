package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ajani's Chosen — Magic 2014 #2
 * {2}{W}{W} · Creature — Cat Soldier · 3/3
 *
 * Whenever an enchantment you control enters, create a 2/2 white Cat creature token. If that
 * enchantment is an Aura, you may attach it to the token.
 *
 * - "That enchantment" is [EffectTarget.TriggeringEntity]; "the token" is the
 *   [CollectionSlot.CreatedTokens] the pipeline's create step just filled.
 * - The Aura check is made at resolution and requires the Aura still be on the battlefield (ruling:
 *   if it left before the ability resolves it won't become attached), so the "you may" is only
 *   offered when there is an Aura to move.
 * - [Effects.AttachTargetEquipmentToCreature] moves the attachment through `AttachmentMover`, which
 *   refuses an attach the Aura's enchant ability doesn't allow (ruling: an "enchant land" Aura
 *   stays put) or a token that has already left the battlefield (ruling), leaving the Aura where it
 *   was.
 */
val AjanisChosen = card("Ajani's Chosen") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Soldier"
    power = 3
    toughness = 3
    oracleText = "Whenever an enchantment you control enters, create a 2/2 white Cat creature token. " +
        "If that enchantment is an Aura, you may attach it to the token."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        effect = Effects.Pipeline {
            run(
                Effects.CreateToken(
                    power = 2,
                    toughness = 2,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Cat"),
                    imageUri = "https://cards.scryfall.io/normal/front/d/b/db41ce71-52df-4bc1-884d-b09419d8dd13.jpg?1783939884"
                )
            )
            run(
                Effects.If(
                    condition = Conditions.EntityMatches(
                        EffectTarget.TriggeringEntity,
                        GameObjectFilter.Enchantment.withSubtype(Subtype.AURA).onBattlefield()
                    ),
                    then = Effects.May(
                        effect = Effects.AttachTargetEquipmentToCreature(
                            equipmentTarget = EffectTarget.TriggeringEntity,
                            creatureTarget = CollectionSlot.CreatedTokens.asTarget
                        )
                    )
                )
            )
        }
        description = "Whenever an enchantment you control enters, create a 2/2 white Cat creature token. " +
            "If that enchantment is an Aura, you may attach it to the token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Wayne Reynolds"
        flavorText = "\"United, our roar will level mountains.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/8/583bfbc1-638b-4de5-b865-0b00a69dd073.jpg?1783939947"
        ruling(
            "2013-07-01",
            "If you cast an Aura targeting an opponent or a permanent controlled by an opponent, you " +
                "control that Aura when it enters the battlefield. The ability of Ajani's Chosen will trigger."
        )
        ruling(
            "2013-07-01",
            "You may attach the Aura to the token only if it can legally enchant that token. For example, " +
                "if the enchantment that entered the battlefield is an Aura with \"enchant land,\" it won't " +
                "become attached to that token."
        )
        ruling(
            "2013-07-01",
            "The ability doesn't trigger until the enchantment enters the battlefield. This means that, for " +
                "example, you can't cast an Aura with enchant creature if there are no legal targets " +
                "available, hoping to enchant the Cat token. Ajani's Chosen itself, however, may be a legal " +
                "target for the Aura."
        )
        ruling(
            "2013-07-01",
            "You may attach the Aura to the Cat token only if the Aura and the token are both on the " +
                "battlefield when the triggered ability resolves. If the Aura leaves the battlefield before " +
                "the ability resolves, it won't become attached to that token. If the token leaves the " +
                "battlefield before the ability resolves, the Aura won't become attached to that token."
        )
    }
}
