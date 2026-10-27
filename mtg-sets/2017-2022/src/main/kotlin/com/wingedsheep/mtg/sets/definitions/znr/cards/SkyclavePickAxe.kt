package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Skyclave Pick-Axe — Zendikar Rising #204
 * {G} · Artifact — Equipment
 *
 * When this Equipment enters, attach it to target creature you control.
 * Landfall — Whenever a land you control enters, equipped creature gets +2/+2 until end of turn.
 * Equip {2}{G}
 *
 * The landfall pump reads the equipped creature as the trigger resolves; with nothing equipped
 * it does nothing.
 */
val SkyclavePickAxe = card("Skyclave Pick-Axe") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, attach it to target creature you control.\n" +
        "Landfall — Whenever a land you control enters, equipped creature gets +2/+2 until end of turn.\n" +
        "Equip {2}{G} ({2}{G}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.youControl())
        effect = Effects.AttachEquipment(t)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.ModifyStats(2, 2, EffectTarget.EquippedCreature)
        description = "Landfall — Whenever a land you control enters, equipped creature gets +2/+2 until end of turn."
    }

    equipAbility("{2}{G}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "204"
        artist = "Dan Murayama Scott"
        flavorText = "For use on cliffs and other faces."
        imageUri = "https://cards.scryfall.io/normal/front/9/9/9965a54b-8b22-418c-9a0d-bf154932f2e2.jpg"
    }
}
