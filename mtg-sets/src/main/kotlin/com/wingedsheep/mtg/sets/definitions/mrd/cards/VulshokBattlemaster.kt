package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vulshok Battlemaster — Mirrodin #110
 * {4}{R} · Creature — Human Warrior · 2/2
 *
 * Haste
 * When this creature enters, attach all Equipment on the battlefield to it.
 * (Control of the Equipment doesn't change.)
 *
 * The trigger sweeps **every** Equipment in play, not just the controller's — the printed
 * rulings are explicit that opponents' Equipment moves too, and that moving it changes neither
 * who controls it nor who may activate its equip ability. So the group filter carries no
 * `youControl()` and no `excludeSelf`; the only constraint is the Equipment subtype.
 *
 * `AttachTargetEquipmentToCreature` is the force-attach atom (Blacksmith's Talent, Beatrix): it
 * detaches from the current host first, so Equipment already strapped to another creature is
 * pulled off exactly as the ruling describes. Inside `ForEachInGroup` the pipeline rebinds
 * [EffectTarget.Self] to the current iteration entity, which is why the Equipment side is `Self`
 * and the Battlemaster is referenced as [EffectTarget.TriggeringEntity] — the entering permanent
 * carried on the enters-the-battlefield `ZoneChangeEvent`.
 *
 * Known gap: the engine's attach atom does not re-check equip legality, so an Equipment whose
 * own restriction excludes the Battlemaster ("equipped creature is a Human", protection from
 * red/artifacts) is still moved onto it rather than staying put per the third printed ruling.
 * That limitation is shared with every other card that force-attaches Equipment, not specific to
 * this one.
 */
val VulshokBattlemaster = card("Vulshok Battlemaster") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 2
    oracleText = "Haste\n" +
        "When this creature enters, attach all Equipment on the battlefield to it. " +
        "(Control of the Equipment doesn't change.)"

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Artifact.withSubtype("Equipment")),
            effect = Effects.AttachTargetEquipmentToCreature(
                equipmentTarget = EffectTarget.Self,
                creatureTarget = EffectTarget.TriggeringEntity
            )
        )
        description = "When this creature enters, attach all Equipment on the battlefield to it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "110"
        artist = "Kev Walker"
        flavorText = "\"I could demonstrate how the leonin sunsplicer works, but then you'd be too dead to buy one.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/8/083f3b83-b7d3-472f-87b2-ed247c9c937e.jpg?1783944537"
        ruling(
            "2004-12-01",
            "The \"enters\" effect moves all Equipment onto the Battlemaster, regardless of whether that Equipment was attached to other creatures."
        )
        ruling(
            "2004-12-01",
            "Other players' Equipment is moved onto the Battlemaster as well as your own. This doesn't change who controls the Equipment or who can activate its equip ability to move it onto another creature."
        )
        ruling(
            "2004-12-01",
            "If an Equipment can't equip Vulshok Battlemaster, it isn't attached to the Battlemaster, and it doesn't become unattached (if it's attached to a creature)."
        )
    }
}
