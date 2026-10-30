package com.wingedsheep.mtg.sets.definitions.brc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Urza, Chief Artificer — The Brothers' War Commander #2
 * {3}{W}{U}{B} · Legendary Creature — Human Artificer · 4/5
 *
 * Affinity for artifact creatures (This spell costs {1} less to cast for each artifact creature
 * you control.)
 * Artifact creatures you control have menace.
 * At the beginning of your end step, create a 0/0 colorless Construct artifact creature token with
 * "This token gets +1/+1 for each artifact you control."
 *
 * - "Affinity for artifact creatures" has no single card type to hang `KeywordAbility.Affinity` on,
 *   so it is the self-cast reduction it abbreviates (Junk Winder's shape) counting
 *   artifact creatures you control. Only generic mana is reduced.
 * - The Construct's bonus is Urza's Saga's token static; it counts itself, so it is at least 1/1
 *   (ruling).
 */
val UrzaChiefArtificer = card("Urza, Chief Artificer") {
    manaCost = "{3}{W}{U}{B}"
    colorIdentity = "WUB"
    typeLine = "Legendary Creature — Human Artificer"
    power = 4
    toughness = 5
    oracleText = "Affinity for artifact creatures (This spell costs {1} less to cast for each " +
        "artifact creature you control.)\n" +
        "Artifact creatures you control have menace.\n" +
        "At the beginning of your end step, create a 0/0 colorless Construct artifact creature token " +
        "with \"This token gets +1/+1 for each artifact you control.\""

    // Affinity for artifact creatures
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.PermanentsYouControlMatching(GameObjectFilter.ArtifactCreature)
            )
        )
    }

    staticAbility {
        ability = GrantKeyword(Keyword.MENACE, GroupFilter(GameObjectFilter.ArtifactCreature.youControl()))
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = emptySet(),
            creatureTypes = setOf("Construct"),
            artifactToken = true,
            staticAbilities = listOf(
                GrantDynamicStats(
                    filter = GroupFilter.source(),
                    powerBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count(),
                    toughnessBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count()
                )
            ),
            imageUri = "https://cards.scryfall.io/normal/front/9/1/914fecab-24c0-4179-84d6-ded78c29134f.jpg?1783919909"
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "2"
        artist = "Bartłomiej Gaweł"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea409050-4296-4b76-a6cd-2896ce1b88e4.jpg?1783919724"
        ruling(
            "2022-10-14",
            "The ability of that Construct token does count the Construct itself, so it'll be at least 1/1."
        )
    }
}
