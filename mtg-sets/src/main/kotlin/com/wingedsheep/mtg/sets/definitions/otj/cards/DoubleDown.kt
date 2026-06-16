package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Double Down
 * {3}{U}
 * Enchantment
 *
 * Whenever you cast an outlaw spell, copy that spell. (Assassins, Mercenaries, Pirates,
 * Rogues, and Warlocks are outlaws. Copies of permanent spells become tokens.)
 *
 * An "outlaw spell" is any spell with one or more of the outlaw creature types
 * ([Subtype.OUTLAW_TYPES]); the trigger uses [GameObjectFilter.Any.withAnyOfSubtypes] so it
 * fires for kindred/creature/any-card-type spells alike. The copy is made of the triggering
 * spell itself ([EffectTarget.TriggeringEntity]) — reusing [Effects.CopyTargetSpell], the same
 * idiom as Alania, Divergent Storm and Breeches, the Blastmaker. The copy is created on the
 * stack (not "cast"), and a copy of a permanent spell becomes a token on resolution; both are
 * handled by the standard copy executor.
 */
val DoubleDown = card("Double Down") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast an outlaw spell, copy that spell. (Assassins, Mercenaries, " +
        "Pirates, Rogues, and Warlocks are outlaws. Copies of permanent spells become tokens.)"

    triggeredAbility {
        trigger = Triggers.youCastSpell(
            spellFilter = GameObjectFilter.Any.withAnyOfSubtypes(Subtype.OUTLAW_TYPES)
        )
        effect = Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity)
        description = "Whenever you cast an outlaw spell, copy that spell."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "44"
        artist = "Javier Charro"
        flavorText = "The best alibi is being in multiple places at the same time."
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8ecccdf3-98c6-4aed-8757-913623efb677.jpg?1712355404"

        ruling("2024-04-12", "Double Down's ability and the copy it creates will resolve before the spell that caused it to trigger. They resolve even if that spell is countered.")
        ruling("2024-04-12", "As a copy of a permanent spell resolves, it's put onto the battlefield as a token rather than putting a copy of the spell onto the battlefield.")
        ruling("2024-04-12", "If a spell has the kindred card type and one of the outlaw creature types, Double Down will copy it.")
        ruling("2024-04-12", "Double Down's ability doesn't trigger if an outlaw permanent is put onto the battlefield without being cast.")
        ruling("2024-04-12", "A card, spell, or permanent is an outlaw if it has the Assassin, Mercenary, Pirate, Rogue, or Warlock creature type.")
    }
}
