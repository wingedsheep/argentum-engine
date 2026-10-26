package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Toralf, God of Fury // Toralf's Hammer — Kaldheim #154 (canonical printing)
 * Legendary Creature — God 5/4 // Legendary Artifact — Equipment · Modal double-faced card
 *
 * Front: an observer trigger on any source's excess noncombat damage to an opponent's creature or
 * planeswalker. The triggering entity is the damaged permanent, so "any target other than that
 * permanent" is an [Targets.Any] narrowed by `notTriggeringEntity()`.
 *
 * Back: the Blazing Torch idiom — the quoted ability is granted to the bearer, so `{T}` taps the
 * creature, "Unattach Toralf's Hammer" is [Costs.UnattachGrantingPermanent], and the Hammer (not the
 * creature) deals the damage and goes back to hand via [EffectTarget.GrantingSource].
 */
private val ToralfGodOfFuryFront = card("Toralf, God of Fury") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — God"
    power = 5
    toughness = 4
    oracleText = "Trample\n" +
        "Whenever a creature or planeswalker an opponent controls is dealt excess noncombat damage, " +
        "Toralf deals damage equal to the excess to any target other than that permanent."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.a().dealsDamage(
            to = Recipient.Object(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()),
            damageType = DamageType.NonCombat,
            requireExcess = true,
        )
        val other = target(Targets.Any(GameObjectFilter.Any.notTriggeringEntity()))
        effect = Effects.DealDamage(DynamicAmounts.triggerExcessDamageAmount(), other)
        description = "Whenever a creature or planeswalker an opponent controls is dealt excess noncombat " +
            "damage, Toralf deals damage equal to the excess to any target other than that permanent."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "154"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22a6a5f1-1405-4efb-af3e-e1f58d664e99.jpg?1783928227"
        ruling("2021-02-05", "A creature has been dealt excess damage if one or more sources deal more damage to it than the minimum amount of damage required to be lethal damage. In most cases, this means damage greater than its toughness, but consider any damage already dealt to it that turn.")
        ruling("2021-02-05", "A planeswalker is dealt excess damage if it's dealt damage greater than its current loyalty.")
        ruling("2021-02-05", "It doesn't matter if a creature or planeswalker an opponent controls was dealt combat damage earlier in the turn. The only thing that matters is that the damage dealt to that permanent that caused excess damage to be dealt was noncombat damage.")
        ruling("2021-02-05", "The damage Toralf deals because of its triggered ability is noncombat damage. That damage may cause Toralf's ability to trigger again.")
    }
}

private val ToralfsHammerBack = card("Toralf's Hammer") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Equipped creature has \"{1}{R}, {T}, Unattach Toralf's Hammer: It deals 3 damage to any " +
        "target. Return Toralf's Hammer to its owner's hand.\"\n" +
        "Equipped creature gets +3/+0 as long as it's legendary.\n" +
        "Equip {1}{R}"

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Composite(
                    Costs.Mana("{1}{R}"),
                    Costs.Tap,
                    Costs.UnattachGrantingPermanent,
                )
                val anyTarget = target(Targets.Any)
                effect = Effects.DealDamage(
                    amount = 3,
                    target = anyTarget,
                    damageSource = EffectTarget.GrantingSource,
                ) then Effects.ReturnToHand(EffectTarget.GrantingSource)
                description = "{1}{R}, {T}, Unattach Toralf's Hammer: It deals 3 damage to any target. " +
                    "Return Toralf's Hammer to its owner's hand."
            },
        )
    }

    staticAbility {
        condition = Conditions.EnchantedPermanentMatches(GameObjectFilter.Creature.legendary())
        ability = ModifyStats(3, 0, Filters.EquippedCreature)
    }

    equipAbility("{1}{R}")

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "154"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/back/2/2/22a6a5f1-1405-4efb-af3e-e1f58d664e99.jpg?1783928227"
        ruling("2021-02-05", "The creature equipped by Toralf's Hammer is the source of the activated ability, but Toralf's Hammer is the source of the damage. For example, if the equipped creature is green, that ability can't target a permanent with protection from green. It could target one with protection from red, though the damage would be prevented as it would have been dealt by a red source (Toralf's Hammer).")
        ruling("2021-02-05", "If the target is an illegal target as the ability tries to resolve, the ability won't resolve and none of its effects will happen. You won't return Toralf's Hammer to its owner's hand.")
        ruling("2021-02-05", "You unattach Toralf's Hammer as part of the cost of activating the ability. If the ability doesn't resolve, Toralf's Hammer doesn't become reattached.")
    }
}

val ToralfGodOfFury: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = ToralfGodOfFuryFront,
    backFace = ToralfsHammerBack,
)
