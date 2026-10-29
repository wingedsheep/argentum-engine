package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Feline Sovereign — Core Set 2021 #180
 * {2}{G} · Creature — Cat · 2/3
 *
 * Other Cats you control get +1/+1 and have protection from Dogs.
 * Whenever one or more Cats you control deal combat damage to a player, destroy up to one target
 * artifact or enchantment that player controls.
 *
 * The lord grants the projected `PROTECTION_FROM_SUBTYPE_DOG` keyword — the string
 * `KeywordAbility.protectionFromSubtype("Dog")` projects, read by targeting, blocking and damage
 * (the Katilda's Rising Dawn shape). The
 * trigger is the batched combat-damage trigger (Nature's Will / Alela): once per damaged player,
 * with "that player" bound as [Player.TriggeringPlayer].
 */
private val otherCatsYouControl =
    GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.CAT).youControl(), excludeSelf = true)

private const val PROTECTION_FROM_DOGS = "PROTECTION_FROM_SUBTYPE_DOG"

val FelineSovereign = card("Feline Sovereign") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat"
    power = 2
    toughness = 3
    oracleText = "Other Cats you control get +1/+1 and have protection from Dogs.\n" +
        "Whenever one or more Cats you control deal combat damage to a player, destroy up to one target " +
        "artifact or enchantment that player controls."

    staticAbility {
        ability = ModifyStats(powerBonus = 1, toughnessBonus = 1, filter = otherCatsYouControl)
    }
    staticAbility {
        ability = GrantKeyword(PROTECTION_FROM_DOGS, otherCatsYouControl)
    }

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature.withSubtype(Subtype.CAT).youControl())
            .dealCombatDamageToAPlayer()
        val permanent = target(
            TargetFilter(
                GameObjectFilter.ArtifactOrEnchantment.targetPlayerControls(
                    EffectTarget.PlayerRef(Player.TriggeringPlayer),
                ),
            ),
            optional = true,
        )
        effect = Effects.Destroy(permanent)
        description = "Whenever one or more Cats you control deal combat damage to a player, destroy up to " +
            "one target artifact or enchantment that player controls."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "180"
        artist = "Dan Murayama Scott"
        flavorText = "Dogs beg. Cats lay claim."
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84a9485a-d356-4cbe-b257-b62008a21328.jpg?1783930677"
        ruling(
            "2020-06-23",
            "Because damage remains marked on a creature until the damage is removed as the turn ends, " +
                "nonlethal damage dealt to a Cat you control may become lethal if Feline Sovereign leaves " +
                "the battlefield during that turn."
        )
    }
}
