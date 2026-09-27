package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * General's Kabuto
 * {4}
 * Artifact — Equipment
 * Equipped creature has shroud. (It can't be the target of spells or abilities.)
 * Prevent all combat damage that would be dealt to equipped creature.
 * Equip {2}
 *
 * The shroud is a [GrantKeyword] static on the equipped creature (as Whispersilk Cloak); the
 * prevention is a continuous [PreventDamage] replacement keyed to [Recipient.EquippedCreature] and
 * scoped to combat damage (as Shield of the Realm, without the amount cap). Noncombat damage — e.g.
 * an untargeted sweeper — still gets through.
 */
val GeneralsKabuto = card("General's Kabuto") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has shroud. (It can't be the target of spells or abilities.)\n" +
        "Prevent all combat damage that would be dealt to equipped creature.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = GrantKeyword(Keyword.SHROUD)
    }

    replacementEffect(
        PreventDamage(
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.EquippedCreature,
                damageType = DamageType.Combat
            )
        )
    )

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "251"
        artist = "Alex Horley-Orlandelli"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/1382c339-256f-4ba1-a4cc-6307d0859964.jpg?1783944280"
    }
}
