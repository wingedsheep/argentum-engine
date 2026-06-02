package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Éowyn, Fearless Knight
 * {2}{R}{W}
 * Legendary Creature — Human Knight
 * 3/4
 *
 * Haste
 * When Éowyn enters, exile target creature an opponent controls with greater power.
 * Legendary creatures you control gain protection from each of that creature's colors
 * until end of turn.
 *
 * The grant fires before the exile in the same resolution so the target's projected
 * colors (including any Layer-5 color-changing effects) are read while it's still on
 * the battlefield; both halves resolve together as one effect (CR 608.2).
 */
val EowynFearlessKnight = card("Éowyn, Fearless Knight") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Knight"
    power = 3
    toughness = 4
    oracleText = "Haste\n" +
        "When Éowyn enters, exile target creature an opponent controls with greater power. " +
        "Legendary creatures you control gain protection from each of that creature's colors " +
        "until end of turn."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val victim = target(
            "creature an opponent controls with greater power",
            TargetCreature(
                filter = TargetFilter(
                    GameObjectFilter.Creature.opponentControls()
                        .powerGreaterThanEntity(EntityReference.Source)
                )
            )
        )
        // Grant before exile so the target's projected colors are read while it's still on the
        // battlefield. ForEachColorOf runs the inner grant once per color of the exiled creature,
        // feeding each color to GrantProtectionFromChosenColor via the chosen-color context.
        effect = Effects.Composite(
            Effects.ForEachColorOf(
                source = EntityReference.Target(0),
                effect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature.legendary().youControl()),
                    Effects.GrantProtectionFromChosenColor(EffectTarget.Self)
                )
            ),
            Effects.Exile(victim)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "201"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/adffb389-c9a3-41c4-b078-292a2bcf870d.jpg?1686969744"
    }
}
