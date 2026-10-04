package com.wingedsheep.mtg.sets.definitions.c19.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sevinne's Reclamation
 * {2}{W}
 * Sorcery
 *
 * Return target permanent card with mana value 3 or less from your graveyard to the battlefield.
 * If this spell was cast from a graveyard, you may copy this spell and may choose a new target for
 * the copy.
 * Flashback {4}{W}
 *
 * The copy is made on resolution with [Effects.CopyTargetSpell] of the resolving spell itself, which
 * prompts for the copy's new target (CR 707.10c). A copy isn't cast (CR 707.10), so it carries no
 * cast-from zone and its own "cast from a graveyard" check is false — it never copies itself again.
 */
val SevinnesReclamation = card("Sevinne's Reclamation") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Return target permanent card with mana value 3 or less from your graveyard to the battlefield. " +
        "If this spell was cast from a graveyard, you may copy this spell and may choose a new target for the copy.\n" +
        "Flashback {4}{W} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        val t = target(TargetFilter(GameObjectFilter.Permanent.ownedByYou().manaValueAtMost(3), zone = Zone.GRAVEYARD))
        effect = Effects.Move(t, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD) then
            Effects.If(
                condition = Conditions.WasCastFromZone(Zone.GRAVEYARD),
                then = Effects.May(Effects.CopyTargetSpell(target = EffectTarget.Self))
            )
    }

    keywordAbility(KeywordAbility.flashback("{4}{W}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "5"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e68f4df-88ce-4e09-a03c-7edf40bff167.jpg?1783932814"
    }
}
