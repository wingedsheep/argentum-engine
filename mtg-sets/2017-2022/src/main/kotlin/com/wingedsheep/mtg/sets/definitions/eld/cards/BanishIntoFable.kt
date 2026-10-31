package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Banish into Fable — Throne of Eldraine #325 (Brawl deck)
 * {4}{W}{U} · Instant
 *
 * When you cast this spell from your hand, copy it if you control an artifact, then copy it if
 * you control an enchantment. You may choose new targets for the copies.
 * Return target nonland permanent to its owner's hand. You create a 2/2 white Knight creature
 * token with vigilance.
 *
 * The cast trigger is `Triggers.self.isCast()` (Mentor's Guidance / Photon Blast Barrage). "From
 * your hand" is part of the trigger event, so it is a `triggerRestriction` reading the spell's own
 * cast origin ([GameObjectFilter] `.cast(Zone.HAND)` → `StatePredicate.WasCast`): cast from a
 * graveyard (flashback) or exile, it never triggers. The artifact / enchantment checks happen only
 * as the trigger resolves (ruling), so each is an [Effects.If] around [Effects.CopyTargetSpell] of
 * the triggering spell — an artifact enchantment satisfies both and copies twice. Copying reads the
 * spell's last-known information when it was countered in response (ruling). Copies aren't cast
 * (CR 707.10), so they don't re-trigger.
 */
val BanishIntoFable = card("Banish into Fable") {
    manaCost = "{4}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Instant"
    oracleText = "When you cast this spell from your hand, copy it if you control an artifact, " +
        "then copy it if you control an enchantment. You may choose new targets for the copies.\n" +
        "Return target nonland permanent to its owner's hand. You create a 2/2 white Knight " +
        "creature token with vigilance."

    triggeredAbility {
        trigger = Triggers.self.isCast()
        triggerRestriction = Conditions.SourceMatches(GameObjectFilter.Any.cast(Zone.HAND))
        effect = Effects.If(
            Conditions.ControlArtifact,
            Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity),
        ) then Effects.If(
            Conditions.ControlEnchantment,
            Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity),
        )
        description = "When you cast this spell from your hand, copy it if you control an " +
            "artifact, then copy it if you control an enchantment. You may choose new targets " +
            "for the copies."
    }

    spell {
        val permanent = target(TargetFilter.NonlandPermanent)
        effect = Effects.ReturnToHand(permanent) then Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Knight"),
            keywords = setOf(Keyword.VIGILANCE),
            imageUri = "https://cards.scryfall.io/normal/front/7/0/703e7ecf-3d73-40c1-8cfe-0758778817cf.jpg?1783932483",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "325"
        artist = "Colin Boyer"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b5e4c5d-7d1d-4426-8351-35c12e29d9c3.jpg?1783932548"
        ruling("2019-10-04", "The triggered ability that copies Banish into Fable resolves and creates copies of Banish into Fable even if Banish into Fable is countered. The ability and the copies resolve before Banish into Fable itself resolves.")
        ruling("2019-10-04", "Whether you control an artifact and/or an enchantment is checked only as the triggered ability resolves.")
        ruling("2019-10-04", "If you control one permanent that's an artifact and an enchantment, you'll copy Banish into Fable twice.")
        ruling("2019-10-04", "If the target nonland permanent is an illegal target by the time Banish into Fable tries to resolve, the spell won't resolve. You won't create a Knight token.")
    }
}
